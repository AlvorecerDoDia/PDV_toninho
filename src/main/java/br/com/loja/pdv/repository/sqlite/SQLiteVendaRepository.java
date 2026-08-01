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

/** Executa venda e cancelamento como transacoes completas do SQLite. */
public final class SQLiteVendaRepository implements VendaRepository {
    // Uma nova conexao e aberta por operacao para manter o repositorio sem estado.
    private final Database database;

    /** Recebe a configuracao de banco usada para abrir conexoes em cada operacao. */
    public SQLiteVendaRepository(Database database) {
        this.database = database;
    }

    /** Persiste venda, itens, pagamentos, estoque e caixa na mesma transacao. */
    @Override
    public Venda finalizar(Venda venda) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Venda, estoque, pagamentos e caixa formam uma unica operacao:
                // qualquer falha desfaz o conjunto para nao deixar saldos divergentes.
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
                insertDiscountAudit(connection, venda);
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

    /** Consulta uma venda pelo identificador. */
    @Override
    public Optional<Venda> buscarPorId(long id) {
        return findSale("SELECT * FROM venda WHERE id = ?",
                statement -> statement.setLong(1, id));
    }

    /** Consulta uma venda pelo numero publico. */
    @Override
    public Optional<Venda> buscarPorNumero(String numero) {
        return findSale("SELECT * FROM venda WHERE numero = ?",
                statement -> statement.setString(1, numero));
    }

    /** Pesquisa vendas usando filtros opcionais. */
    @Override
    public List<Venda> listar(
            LocalDateTime inicio, LocalDateTime fim, Long operadorId) {
        String sql = """
                SELECT * FROM venda
                WHERE criado_em >= ? AND criado_em <= ?
                  AND (? IS NULL OR operador_id = ?)
                ORDER BY criado_em DESC, id DESC
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, inicio.toString());
            statement.setString(2, fim.toString());
            if (operadorId == null) {
                statement.setNull(3, Types.BIGINT);
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(3, operadorId);
                statement.setLong(4, operadorId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Venda> result = new ArrayList<>();
                while (resultSet.next()) result.add(mapSale(resultSet));
                return result;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar as vendas.", exception);
        }
    }

    /** Retorna os itens historicos de uma venda. */
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

    /** Estorna estoque e caixa, marca a venda e registra auditoria na mesma transacao. */
    @Override
    public Venda cancelar(
            long vendaId, long usuarioId, String motivo, LocalDateTime canceladoEm) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // O cancelamento so e concluido depois de devolver todos os itens,
                // estornar o caixa e registrar a auditoria na mesma transacao.
                Venda venda = findSale(connection, vendaId);
                if (venda.getStatus() == StatusVenda.CANCELADA) {
                    throw new ValidationException("A venda já foi cancelada.");
                }
                markCanceled(connection, vendaId, motivo, canceladoEm);
                List<ItemVenda> items = listItems(connection, vendaId);
                for (ItemVenda item : items) {
                    restoreStock(connection, venda, item, usuarioId, motivo, canceladoEm);
                }
                BigDecimal cashToReverse = cashRetained(connection, venda);
                if (cashToReverse.signum() > 0) {
                    reverseCash(connection, venda, usuarioId, motivo, canceladoEm, cashToReverse);
                }
                insertCancellationAudit(
                        connection, venda, usuarioId, motivo, canceladoEm);
                connection.commit();
                venda.setStatus(StatusVenda.CANCELADA);
                venda.setCanceladoEm(canceladoEm);
                venda.setMotivoCancelamento(motivo);
                return venda;
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new DatabaseException("Não foi possível cancelar a venda.", exception);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível acessar a venda.", exception);
        }
    }

    /** Executa a consulta base e monta a venda com pagamentos. */
    private Optional<Venda> findSale(String sql, StatementBinder binder) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapSale(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar a venda.", exception);
        }
    }

    /** Executa a consulta base e monta a venda com pagamentos. */
    private Venda findSale(Connection connection, long vendaId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM venda WHERE id = ?")) {
            statement.setLong(1, vendaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new ValidationException("Venda não encontrada.");
                return mapSale(resultSet);
            }
        }
    }

    /** Le todos os itens associados a uma venda. */
    private List<ItemVenda> listItems(Connection connection, long vendaId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM item_venda WHERE venda_id = ? ORDER BY id")) {
            statement.setLong(1, vendaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ItemVenda> result = new ArrayList<>();
                while (resultSet.next()) result.add(mapItem(resultSet));
                return result;
            }
        }
    }

    /** Atualiza status, data e motivo do cancelamento. */
    private void markCanceled(
            Connection connection, long vendaId, String motivo, LocalDateTime canceledAt)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE venda SET status = 'CANCELADA', cancelado_em = ?,
                    motivo_cancelamento = ?
                WHERE id = ? AND status = 'FINALIZADA'
                """)) {
            statement.setString(1, canceledAt.toString());
            statement.setString(2, motivo);
            statement.setLong(3, vendaId);
            if (statement.executeUpdate() != 1) {
                throw new ValidationException("A venda já foi cancelada.");
            }
        }
    }

    /** Devolve ao estoque as quantidades de todos os itens. */
    private void restoreStock(
            Connection connection, Venda venda, ItemVenda item, long usuarioId,
            String motivo, LocalDateTime canceledAt) throws SQLException {
        int previous;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT quantidade_estoque FROM produto WHERE id = ?")) {
            statement.setLong(1, item.getProdutoId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new ValidationException(
                            "Produto da venda não foi encontrado: " + item.getProdutoNome() + ".");
                }
                previous = resultSet.getInt(1);
            }
        }
        int next = Math.addExact(previous, item.getQuantidade());
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE produto SET quantidade_estoque = ?, atualizado_em = ? WHERE id = ?
                """)) {
            statement.setInt(1, next);
            statement.setString(2, canceledAt.toString());
            statement.setLong(3, item.getProdutoId());
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO movimentacao_estoque (
                    produto_id, tipo, quantidade, quantidade_anterior,
                    quantidade_posterior, motivo, usuario_id, venda_id, criado_em
                ) VALUES (?, 'DEVOLUCAO', ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, item.getProdutoId());
            statement.setInt(2, item.getQuantidade());
            statement.setInt(3, previous);
            statement.setInt(4, next);
            statement.setString(5, "Cancelamento da venda " + venda.getNumero()
                    + ": " + motivo);
            statement.setLong(6, usuarioId);
            statement.setLong(7, venda.getId());
            statement.setString(8, canceledAt.toString());
            statement.executeUpdate();
        }
    }

    /** Calcula quanto dinheiro permaneceu no caixa depois do troco. */
    private BigDecimal cashRetained(Connection connection, Venda venda) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COALESCE(SUM(valor_centavos), 0)
                FROM pagamento WHERE venda_id = ? AND forma = 'DINHEIRO'
                """)) {
            statement.setLong(1, venda.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return MoneyUtils.fromCents(resultSet.getLong(1)).subtract(venda.getTroco());
            }
        }
    }

    /** Registra a movimentacao inversa quando o cancelamento afeta dinheiro. */
    private void reverseCash(
            Connection connection, Venda venda, long usuarioId, String motivo,
            LocalDateTime canceledAt, BigDecimal value) throws SQLException {
        long cents = MoneyUtils.toCents(value);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO movimentacao_caixa (
                    caixa_id, usuario_id, tipo, valor_centavos, motivo, criado_em
                ) VALUES (?, ?, 'ESTORNO', ?, ?, ?)
                """)) {
            statement.setLong(1, venda.getCaixaId());
            statement.setLong(2, usuarioId);
            statement.setLong(3, cents);
            statement.setString(4, "Cancelamento da venda " + venda.getNumero()
                    + ": " + motivo);
            statement.setString(5, canceledAt.toString());
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE caixa
                SET valor_esperado_centavos = valor_esperado_centavos - ?,
                    diferenca_centavos = valor_contado_centavos
                        - (valor_esperado_centavos - ?)
                WHERE id = ? AND status = 'FECHADO'
                  AND valor_esperado_centavos IS NOT NULL
                  AND valor_contado_centavos IS NOT NULL
                """)) {
            statement.setLong(1, cents);
            statement.setLong(2, cents);
            statement.setLong(3, venda.getCaixaId());
            statement.executeUpdate();
        }
    }

    /** Registra os dados essenciais do cancelamento para rastreabilidade. */
    private void insertCancellationAudit(
            Connection connection, Venda venda, long usuarioId, String motivo,
            LocalDateTime canceledAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO auditoria (
                    usuario_id, acao, entidade, entidade_id,
                    valores_anteriores, valores_novos, criado_em
                ) VALUES (?, 'CANCELAMENTO', 'VENDA', ?, ?, ?, ?)
                """)) {
            statement.setLong(1, usuarioId);
            statement.setLong(2, venda.getId());
            statement.setString(3, "status=FINALIZADA");
            statement.setString(4, "status=CANCELADA; motivo=" + motivo);
            statement.setString(5, canceledAt.toString());
            statement.executeUpdate();
        }
    }

    /** Registra descontos aplicados durante a venda. */
    private void insertDiscountAudit(Connection connection, Venda venda)
            throws SQLException {
        if (venda.getDesconto().signum() == 0) return;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO auditoria (
                    usuario_id, acao, entidade, entidade_id,
                    valores_anteriores, valores_novos, criado_em
                ) VALUES (?, 'DESCONTO', 'VENDA', ?, ?, ?, ?)
                """)) {
            statement.setLong(1, venda.getOperadorId());
            statement.setLong(2, venda.getId());
            statement.setString(3, "subtotal="
                    + venda.getSubtotal().toPlainString());
            statement.setString(4, "desconto="
                    + venda.getDesconto().toPlainString()
                    + "; total=" + venda.getTotal().toPlainString());
            statement.setString(5, venda.getCriadoEm().toString());
            statement.executeUpdate();
        }
    }

    /** Confirma que a venda esta vinculada a um caixa ainda aberto. */
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

    /** Bloqueia produtos, valida saldo e copia precos historicos para os itens. */
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

    /** Insere o cabecalho da venda e recupera sua chave. */
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

    /** Insere um item com nome, custo e preco capturados no momento da venda. */
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

    /** Insere uma parcela de pagamento. */
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

    /** Baixa o saldo e grava o historico de saida para cada item. */
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

    /** Registra no caixa apenas a parte recebida em dinheiro. */
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

    /** Converte a linha JDBC no cabecalho da venda. */
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

    /** Converte a linha JDBC em item historico. */
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

    /** Desfaz toda a venda ou cancelamento quando qualquer etapa falha. */
    private void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            cause.addSuppressed(exception);
        }
    }

    /** Permite configurar PreparedStatements sem duplicar o fluxo de consulta. */
    @FunctionalInterface
    private interface StatementBinder {
        /** Preenche os parametros antes de executar a consulta. */
        void bind(PreparedStatement statement) throws SQLException;
    }
}
