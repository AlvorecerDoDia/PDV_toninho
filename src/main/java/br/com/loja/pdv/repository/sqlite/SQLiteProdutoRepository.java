package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.model.Categoria;
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

/** Implementa o catalogo de produtos e traduz restricoes do SQLite. */
public final class SQLiteProdutoRepository implements ProdutoRepository {
    private static final String BASE_SELECT = """
            SELECT p.*, c.nome AS categoria_nome, c.ativa AS categoria_ativa,
                   c.criado_em AS categoria_criado_em,
                   c.atualizado_em AS categoria_atualizado_em
            FROM produto p
            JOIN categoria c ON c.id = p.categoria_id
            """;

    private final Database database;

    /** Recebe a configuracao de banco usada para abrir conexoes em cada operacao. */
    public SQLiteProdutoRepository(Database database) {
        this.database = database;
    }

    /** Insere um produto completo e recupera o identificador criado. */
    @Override
    public Produto salvar(Produto produto) {
        String sql = """
                INSERT INTO produto (
                    codigo_barras, nome, categoria_id,
                    preco_custo_centavos, preco_venda_centavos,
                    quantidade_estoque, estoque_minimo, ativo,
                    criado_em, atualizado_em
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = database.getConnection()) {
            resolveCategory(connection, produto);
            try (PreparedStatement statement = connection.prepareStatement(
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
            }
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    /** Atualiza dados cadastrais sem modificar o saldo de estoque. */
    @Override
    public void atualizar(Produto produto) {
        String sql = """
                UPDATE produto SET codigo_barras = ?, nome = ?, categoria_id = ?,
                    preco_custo_centavos = ?, preco_venda_centavos = ?,
                    quantidade_estoque = ?, estoque_minimo = ?, ativo = ?,
                    criado_em = ?, atualizado_em = ?
                WHERE id = ?
                """;
        try (Connection connection = database.getConnection()) {
            resolveCategory(connection, produto);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, produto);
                statement.setLong(11, produto.getId());
                if (statement.executeUpdate() == 0) {
                    throw new EntityNotFoundException("Produto não encontrado.");
                }
            }
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    /** Consulta um produto pelo identificador. */
    @Override
    public Optional<Produto> buscarPorId(long id) {
        return findOne(BASE_SELECT + " WHERE p.id = ?", statement -> statement.setLong(1, id));
    }

    /** Consulta um produto pelo codigo normalizado. */
    @Override
    public Optional<Produto> buscarPorCodigoBarras(String codigo) {
        return findOne(
                BASE_SELECT + " WHERE p.codigo_barras = ?",
                statement -> statement.setString(1, codigo));
    }

    /** Lista somente produtos disponiveis para venda. */
    @Override
    public List<Produto> listarAtivos() {
        return list(BASE_SELECT + " WHERE p.ativo = 1 ORDER BY p.nome", null);
    }

    /** Busca por nome, codigo ou categoria, incluindo inativos para gestao. */
    @Override
    public List<Produto> pesquisar(String termo) {
        String pattern = "%" + (termo == null ? "" : termo.strip().toLowerCase()) + "%";
        return list(BASE_SELECT + """
                 WHERE LOWER(p.nome) LIKE ?
                    OR LOWER(COALESCE(p.codigo_barras, '')) LIKE ?
                    OR LOWER(c.nome) LIKE ?
                 ORDER BY p.ativo DESC, p.nome
                """, statement -> {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
        });
    }

    /** Marca o produto como inativo sem apagar seu historico. */
    @Override
    public void desativar(long id) {
        updateStatus(id, false);
    }

    /** Volta a disponibilizar o produto para operacoes. */
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
        return list(sql, binder).stream().findFirst();
    }

    private List<Produto> list(String sql, SqlBinder binder) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Produto> products = new ArrayList<>();
                while (resultSet.next()) products.add(map(resultSet));
                return products;
            }
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    /** Preenche os parametros comuns de insercao e atualizacao. */
    private void bind(PreparedStatement statement, Produto produto) throws SQLException {
        statement.setString(1, produto.getCodigoBarras());
        statement.setString(2, produto.getNome());
        statement.setLong(3, produto.getCategoria().getId());
        statement.setLong(4, MoneyUtils.toCents(produto.getPrecoCusto()));
        statement.setLong(5, MoneyUtils.toCents(produto.getPrecoVenda()));
        statement.setInt(6, produto.getQuantidadeEstoque());
        statement.setInt(7, produto.getEstoqueMinimo());
        statement.setBoolean(8, produto.isAtivo());
        statement.setString(9, produto.getCriadoEm().toString());
        statement.setString(10, produto.getAtualizadoEm().toString());
    }

    /** Usa Sem categoria apenas para manter compatibilidade com fluxos antigos e testes. */
    private void resolveCategory(Connection connection, Produto produto) throws SQLException {
        if (produto.getCategoria() != null && produto.getCategoria().getId() != null) return;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM categoria WHERE nome = 'Sem categoria' COLLATE NOCASE");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new DatabaseException("A categoria padrão não foi encontrada.");
            }
            Categoria categoria = new Categoria();
            categoria.setId(resultSet.getLong("id"));
            categoria.setNome(resultSet.getString("nome"));
            categoria.setAtiva(resultSet.getBoolean("ativa"));
            categoria.setCriadoEm(LocalDateTime.parse(resultSet.getString("criado_em")));
            categoria.setAtualizadoEm(LocalDateTime.parse(resultSet.getString("atualizado_em")));
            produto.setCategoria(categoria);
        }
    }

    /** Converte a linha JDBC em Produto e inclui sua categoria. */
    private Produto map(ResultSet resultSet) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setId(resultSet.getLong("categoria_id"));
        categoria.setNome(resultSet.getString("categoria_nome"));
        categoria.setAtiva(resultSet.getBoolean("categoria_ativa"));
        categoria.setCriadoEm(LocalDateTime.parse(resultSet.getString("categoria_criado_em")));
        categoria.setAtualizadoEm(LocalDateTime.parse(resultSet.getString("categoria_atualizado_em")));

        Produto produto = new Produto();
        produto.setId(resultSet.getLong("id"));
        produto.setCodigoBarras(resultSet.getString("codigo_barras"));
        produto.setNome(resultSet.getString("nome"));
        produto.setCategoria(categoria);
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
