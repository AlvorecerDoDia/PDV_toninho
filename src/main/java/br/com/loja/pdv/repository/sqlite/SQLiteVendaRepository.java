package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.enums.StatusVenda;
import br.com.loja.pdv.domain.model.ItemVenda;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.domain.model.Venda;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.repository.VendaRepository;
import br.com.loja.pdv.util.MoneyUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SQLiteVendaRepository implements VendaRepository {
    private final Database database;

    public SQLiteVendaRepository(Database database) {
        this.database = database;
    }

    @Override
    public Venda finalizar(Venda venda) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureOpenCashRegister(connection, venda);
                validateAndCaptureStock(connection, venda);
                insertSale(connection, venda);
                for (ItemVenda item : venda.getItens()) {
                    insertItem(connection, venda.getId(), item);
                    reduceStockAndRegisterMovement(connection, venda, item);
                }
                for (Pagamento payment : venda.getPagamentos()) {
                    insertPayment(connection, venda.getId(), payment);
                }
                insertCashMovement(connection, venda);
                connection.commit();
                return venda;
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new DatabaseException("Não foi possível finalizar a venda.", exception);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível acessar as vendas.", exception);
        }
    }

    @Override
    public Optional<Venda> buscarPorId(long id) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM venda WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapSale(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar a venda.", exception);
        }
    }

    @Override
    public List<ItemVenda> listarItens(long vendaId) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM item_venda WHERE venda_id = ? ORDER BY id")) {
            statement.setLong(1, vendaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ItemVenda> result = new ArrayList<>();
                while (resultSet.next()) result.add(mapItem(resultSet));
                return result;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar os itens da venda.", exception);
        }
    }

    private void ensureOpenCashRegister(Connection connection, Venda venda) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM caixa
                WHERE id = ? AND usuario_id = ? AND status = 'ABERTO'
                """)) {
            statement.setLong(1, venda.getCaixaId());
            statement.setLong(2, venda.getOperadorId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new ValidationException("A venda exige um caixa aberto do operador.");
                }
            }
        }
    }

    private void validateAndCaptureStock(Connection connection, Venda venda) throws SQLException {
        String sql = """
                SELECT nome, preco_custo_centavos, preco_venda_centavos,
                       quantidade_estoque, ativo
                FROM produto WHERE id = ?
                """;
        for (ItemVenda item : venda.getItens()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, item.getProdutoId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next() || !resultSet.getBoolean("ativo")) {
                        throw new ValidationException("Produto indisponível: " + item.getProdutoNome() + ".");
                    }
                    if (resultSet.getInt("quantidade_estoque") < item.getQuantidade()) {
                        throw new ValidationException(
                                "Estoque insuficiente para " + item.getProdutoNome() + ".");
                    }
                    BigDecimal currentPrice = MoneyUtils.fromCents(
                            resultSet.getLong("preco_venda_centavos"));
                    if (currentPrice.compareTo(item.getPrecoUnitario()) != 0) {
                        throw new ValidationException(
                                "O preço de " + item.getProdutoNome()
                                        + " foi alterado. Atualize o carrinho.");
                    }
                    item.setProdutoNome(resultSet.getString("nome"));
                    item.setCustoUnitario(MoneyUtils.fromCents(
                            resultSet.getLong("preco_custo_centavos")));
                }
            }
        }
    }

    private void insertSale(Connection connection, Venda venda) throws SQLException {
        String sql = """
                INSERT INTO venda (
                    numero, operador_id, caixa_id, status, subtotal_centavos,
                    desconto_centavos, total_centavos, troco_centavos, criado_em
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, venda.getNumero());
            statement.setLong(2, venda.getOperadorId());
            statement.setLong(3, venda.getCaixaId());
            statement.setString(4, venda.getStatus().name());
            statement.setLong(5, MoneyUtils.toCents(venda.getSubtotal()));
            statement.setLong(6, MoneyUtils.toCents(venda.getDesconto()));
            statement.setLong(7, MoneyUtils.toCents(venda.getTotal()));
            statement.setLong(8, MoneyUtils.toCents(venda.getTroco()));
            statement.setString(9, venda.getCriadoEm().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("ID da venda não retornado.");
                venda.setId(keys.getLong(1));
            }
        }
    }

    private void insertItem(Connection connection, long vendaId, ItemVenda item)
            throws SQLException {
        String sql = """
                INSERT INTO item_venda (
                    venda_id, produto_id, produto_nome, quantidade,
                    custo_unitario_centavos, preco_unitario_centavos, subtotal_centavos
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, vendaId);
            statement.setLong(2, item.getProdutoId());
            statement.setString(3, item.getProdutoNome());
            statement.setInt(4, item.getQuantidade());
            statement.setLong(5, MoneyUtils.toCents(item.getCustoUnitario()));
            statement.setLong(6, MoneyUtils.toCents(item.getPrecoUnitario()));
            statement.setLong(7, MoneyUtils.toCents(item.getSubtotal()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getLong(1));
            }
            item.setVendaId(vendaId);
        }
    }

    private void insertPayment(Connection connection, long vendaId, Pagamento payment)
            throws SQLException {
        String sql = """
                INSERT INTO pagamento (venda_id, forma, valor_centavos, criado_em)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, vendaId);
            statement.setString(2, payment.getForma().name());
            statement.setLong(3, MoneyUtils.toCents(payment.getValor()));
            statement.setString(4, payment.getCriadoEm().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) payment.setId(keys.getLong(1));
            }
            payment.setVendaId(vendaId);
        }
    }

    private void reduceStockAndRegisterMovement(
            Connection connection, Venda venda, ItemVenda item) throws SQLException {
        int previous;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT quantidade_estoque FROM produto WHERE id = ?")) {
            statement.setLong(1, item.getProdutoId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new ValidationException("Produto não encontrado.");
                previous = resultSet.getInt(1);
            }
        }
        int next = previous - item.getQuantidade();
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE produto SET quantidade_estoque = ?, atualizado_em = ?
                WHERE id = ? AND ativo = 1 AND quantidade_estoque >= ?
                """)) {
            statement.setInt(1, next);
            statement.setString(2, venda.getCriadoEm().toString());
            statement.setLong(3, item.getProdutoId());
            statement.setInt(4, item.getQuantidade());
            if (statement.executeUpdate() != 1) {
                throw new ValidationException(
                        "Estoque insuficiente para " + item.getProdutoNome() + ".");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO movimentacao_estoque (
                    produto_id, tipo, quantidade, quantidade_anterior,
                    quantidade_posterior, motivo, usuario_id, venda_id, criado_em
                ) VALUES (?, 'SAIDA_VENDA', ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, item.getProdutoId());
            statement.setInt(2, item.getQuantidade());
            statement.setInt(3, previous);
            statement.setInt(4, next);
            statement.setString(5, "Venda " + venda.getNumero());
            statement.setLong(6, venda.getOperadorId());
            statement.setLong(7, venda.getId());
            statement.setString(8, venda.getCriadoEm().toString());
            statement.executeUpdate();
        }
    }

    private void insertCashMovement(Connection connection, Venda venda) throws SQLException {
        BigDecimal received = venda.getPagamentos().stream()
                .filter(payment -> payment.getForma()
                        == br.com.loja.pdv.domain.enums.FormaPagamento.DINHEIRO)
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        BigDecimal retained = received.subtract(venda.getTroco());
        if (retained.signum() == 0) return;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO movimentacao_caixa (
                    caixa_id, usuario_id, tipo, valor_centavos, motivo, criado_em
                ) VALUES (?, ?, 'VENDA_DINHEIRO', ?, ?, ?)
                """)) {
            statement.setLong(1, venda.getCaixaId());
            statement.setLong(2, venda.getOperadorId());
            statement.setLong(3, MoneyUtils.toCents(retained));
            statement.setString(4, "Venda " + venda.getNumero());
            statement.setString(5, venda.getCriadoEm().toString());
            statement.executeUpdate();
        }
    }

    private Venda mapSale(ResultSet resultSet) throws SQLException {
        Venda venda = new Venda();
        venda.setId(resultSet.getLong("id"));
        venda.setNumero(resultSet.getString("numero"));
        venda.setOperadorId(resultSet.getLong("operador_id"));
        venda.setCaixaId(resultSet.getLong("caixa_id"));
        venda.setStatus(StatusVenda.valueOf(resultSet.getString("status")));
        venda.setSubtotal(MoneyUtils.fromCents(resultSet.getLong("subtotal_centavos")));
        venda.setDesconto(MoneyUtils.fromCents(resultSet.getLong("desconto_centavos")));
        venda.setTotal(MoneyUtils.fromCents(resultSet.getLong("total_centavos")));
        venda.setTroco(MoneyUtils.fromCents(resultSet.getLong("troco_centavos")));
        venda.setCriadoEm(LocalDateTime.parse(resultSet.getString("criado_em")));
        String canceledAt = resultSet.getString("cancelado_em");
        venda.setCanceladoEm(canceledAt == null ? null : LocalDateTime.parse(canceledAt));
        venda.setMotivoCancelamento(resultSet.getString("motivo_cancelamento"));
        return venda;
    }

    private ItemVenda mapItem(ResultSet resultSet) throws SQLException {
        ItemVenda item = new ItemVenda();
        item.setId(resultSet.getLong("id"));
        item.setVendaId(resultSet.getLong("venda_id"));
        item.setProdutoId(resultSet.getLong("produto_id"));
        item.setProdutoNome(resultSet.getString("produto_nome"));
        item.setQuantidade(resultSet.getInt("quantidade"));
        item.setCustoUnitario(MoneyUtils.fromCents(
                resultSet.getLong("custo_unitario_centavos")));
        item.setPrecoUnitario(MoneyUtils.fromCents(
                resultSet.getLong("preco_unitario_centavos")));
        item.setSubtotal(MoneyUtils.fromCents(resultSet.getLong("subtotal_centavos")));
        return item;
    }

    private void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            cause.addSuppressed(exception);
        }
    }
}
