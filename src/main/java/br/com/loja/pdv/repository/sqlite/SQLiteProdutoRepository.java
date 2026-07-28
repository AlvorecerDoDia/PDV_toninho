package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.DuplicateBarcodeException;
import br.com.loja.pdv.exception.EntityNotFoundException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.repository.ProdutoRepository;
import br.com.loja.pdv.util.MoneyUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SQLiteProdutoRepository implements ProdutoRepository {

    private final Database database;

    public SQLiteProdutoRepository(Database database) {
        this.database = database;
    }

    @Override
    public Produto salvar(Produto produto) {
        String sql = """
                INSERT INTO produto (
                    codigo_barras, nome, preco_custo_centavos, preco_venda_centavos,
                    quantidade_estoque, estoque_minimo, ativo, criado_em, atualizado_em
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, produto);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("O banco não retornou o ID do produto.");
                }
                produto.setId(keys.getLong(1));
            }
            return produto;
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    @Override
    public void atualizar(Produto produto) {
        String sql = """
                UPDATE produto SET codigo_barras = ?, nome = ?,
                    preco_custo_centavos = ?, preco_venda_centavos = ?,
                    quantidade_estoque = ?, estoque_minimo = ?, ativo = ?,
                    criado_em = ?, atualizado_em = ?
                WHERE id = ?
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, produto);
            statement.setLong(10, produto.getId());
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Produto não encontrado.");
            }
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    @Override
    public Optional<Produto> buscarPorId(long id) {
        return findOne("SELECT * FROM produto WHERE id = ?", statement -> statement.setLong(1, id));
    }

    @Override
    public Optional<Produto> buscarPorCodigoBarras(String codigo) {
        return findOne(
                "SELECT * FROM produto WHERE codigo_barras = ?",
                statement -> statement.setString(1, codigo)
        );
    }

    @Override
    public List<Produto> listarAtivos() {
        return list("SELECT * FROM produto WHERE ativo = 1 ORDER BY nome", null);
    }

    @Override
    public List<Produto> pesquisar(String termo) {
        String pattern = "%" + (termo == null ? "" : termo.strip().toLowerCase()) + "%";
        return list("""
                SELECT * FROM produto
                WHERE LOWER(nome) LIKE ? OR LOWER(COALESCE(codigo_barras, '')) LIKE ?
                ORDER BY ativo DESC, nome
                """, statement -> {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
        });
    }

    @Override
    public void desativar(long id) {
        updateStatus(id, false);
    }

    @Override
    public void reativar(long id) {
        updateStatus(id, true);
    }

    private void updateStatus(long id, boolean active) {
        String sql = "UPDATE produto SET ativo = ?, atualizado_em = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, active);
            statement.setString(2, LocalDateTime.now().toString());
            statement.setLong(3, id);
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Produto não encontrado.");
            }
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    private Optional<Produto> findOne(String sql, SqlBinder binder) {
        List<Produto> products = list(sql, binder);
        return products.stream().findFirst();
    }

    private List<Produto> list(String sql, SqlBinder binder) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Produto> products = new ArrayList<>();
                while (resultSet.next()) {
                    products.add(map(resultSet));
                }
                return products;
            }
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    private void bind(PreparedStatement statement, Produto produto) throws SQLException {
        statement.setString(1, produto.getCodigoBarras());
        statement.setString(2, produto.getNome());
        statement.setLong(3, MoneyUtils.toCents(produto.getPrecoCusto()));
        statement.setLong(4, MoneyUtils.toCents(produto.getPrecoVenda()));
        statement.setInt(5, produto.getQuantidadeEstoque());
        statement.setInt(6, produto.getEstoqueMinimo());
        statement.setBoolean(7, produto.isAtivo());
        statement.setString(8, produto.getCriadoEm().toString());
        statement.setString(9, produto.getAtualizadoEm().toString());
    }

    private Produto map(ResultSet resultSet) throws SQLException {
        Produto produto = new Produto();
        produto.setId(resultSet.getLong("id"));
        produto.setCodigoBarras(resultSet.getString("codigo_barras"));
        produto.setNome(resultSet.getString("nome"));
        produto.setPrecoCusto(MoneyUtils.fromCents(resultSet.getLong("preco_custo_centavos")));
        produto.setPrecoVenda(MoneyUtils.fromCents(resultSet.getLong("preco_venda_centavos")));
        produto.setQuantidadeEstoque(resultSet.getInt("quantidade_estoque"));
        produto.setEstoqueMinimo(resultSet.getInt("estoque_minimo"));
        produto.setAtivo(resultSet.getBoolean("ativo"));
        produto.setCriadoEm(LocalDateTime.parse(resultSet.getString("criado_em")));
        produto.setAtualizadoEm(LocalDateTime.parse(resultSet.getString("atualizado_em")));
        return produto;
    }

    private RuntimeException translate(SQLException exception) {
        if (exception.getErrorCode() == 19
                && exception.getMessage().toLowerCase().contains("codigo_barras")) {
            return new DuplicateBarcodeException("Já existe um produto com esse código de barras.");
        }
        return new DatabaseException("Não foi possível acessar os produtos.", exception);
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
