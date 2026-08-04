package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.model.Categoria;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.EntityNotFoundException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.repository.CategoriaRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persiste as categorias usadas pelo catalogo de produtos. */
public final class SQLiteCategoriaRepository implements CategoriaRepository {
    private final Database database;

    public SQLiteCategoriaRepository(Database database) {
        this.database = database;
    }

    @Override
    public Categoria salvar(Categoria categoria) {
        String sql = """
                INSERT INTO categoria (nome, ativa, criado_em, atualizado_em)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, categoria);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("O banco não retornou o ID da categoria.");
                }
                categoria.setId(keys.getLong(1));
            }
            return categoria;
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    @Override
    public void atualizar(Categoria categoria) {
        String sql = """
                UPDATE categoria
                SET nome = ?, ativa = ?, criado_em = ?, atualizado_em = ?
                WHERE id = ?
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, categoria);
            statement.setLong(5, categoria.getId());
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Categoria não encontrada.");
            }
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    @Override
    public Optional<Categoria> buscarPorId(long id) {
        return findOne("SELECT * FROM categoria WHERE id = ?", statement -> statement.setLong(1, id));
    }

    @Override
    public Optional<Categoria> buscarPorNome(String nome) {
        return findOne(
                "SELECT * FROM categoria WHERE nome = ? COLLATE NOCASE",
                statement -> statement.setString(1, nome));
    }

    @Override
    public List<Categoria> listarAtivas() {
        return list("SELECT * FROM categoria WHERE ativa = 1 ORDER BY nome", null);
    }

    @Override
    public List<Categoria> listarTodas() {
        return list("SELECT * FROM categoria ORDER BY ativa DESC, nome", null);
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
        String sql = "UPDATE categoria SET ativa = ?, atualizado_em = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, active);
            statement.setString(2, LocalDateTime.now().toString());
            statement.setLong(3, id);
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Categoria não encontrada.");
            }
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    private Optional<Categoria> findOne(String sql, SqlBinder binder) {
        return list(sql, binder).stream().findFirst();
    }

    private List<Categoria> list(String sql, SqlBinder binder) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Categoria> categories = new ArrayList<>();
                while (resultSet.next()) categories.add(map(resultSet));
                return categories;
            }
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    private void bind(PreparedStatement statement, Categoria categoria) throws SQLException {
        statement.setString(1, categoria.getNome());
        statement.setBoolean(2, categoria.isAtiva());
        statement.setString(3, categoria.getCriadoEm().toString());
        statement.setString(4, categoria.getAtualizadoEm().toString());
    }

    private Categoria map(ResultSet resultSet) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setId(resultSet.getLong("id"));
        categoria.setNome(resultSet.getString("nome"));
        categoria.setAtiva(resultSet.getBoolean("ativa"));
        categoria.setCriadoEm(LocalDateTime.parse(resultSet.getString("criado_em")));
        categoria.setAtualizadoEm(LocalDateTime.parse(resultSet.getString("atualizado_em")));
        return categoria;
    }

    private RuntimeException translate(SQLException exception) {
        return new DatabaseException("Não foi possível acessar as categorias.", exception);
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
