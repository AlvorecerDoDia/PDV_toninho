package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.model.Categoria;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.EntityNotFoundException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.repository.CategoriaRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persiste categorias usando a estrutura existente do banco. */
public final class SQLiteCategoriaRepository implements CategoriaRepository {
    private final Database database;

    public SQLiteCategoriaRepository(Database database) {
        this.database = database;
    }

    @Override
    public Categoria salvar(Categoria categoria) {
        String sql = """
                INSERT INTO categoria (nome, ativa, criado_em, atualizado_em)
                VALUES (?, 1, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, categoria.getNome());
            statement.setString(2, categoria.getCriadoEm().toString());
            statement.setString(3, categoria.getAtualizadoEm().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("O banco não retornou o ID da categoria.");
                }
                categoria.setId(keys.getLong(1));
            }
            return categoria;
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível salvar a categoria.", exception);
        }
    }

    @Override
    public void atualizar(Categoria categoria) {
        String sql = """
                UPDATE categoria SET nome = ?, ativa = 1, atualizado_em = ? WHERE id = ?
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, categoria.getNome());
            statement.setString(2, categoria.getAtualizadoEm().toString());
            statement.setLong(3, categoria.getId());
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Categoria não encontrada.");
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível atualizar a categoria.", exception);
        }
    }

    @Override
    public Optional<Categoria> buscarPorId(long id) {
        return um("SELECT * FROM categoria WHERE id = ?", statement -> statement.setLong(1, id));
    }

    @Override
    public Optional<Categoria> buscarPorNome(String nome) {
        return um("SELECT * FROM categoria WHERE nome = ? COLLATE NOCASE",
                statement -> statement.setString(1, nome));
    }

    @Override
    public List<Categoria> listarTodas() {
        return listar("SELECT * FROM categoria ORDER BY nome", null);
    }

    private Optional<Categoria> um(String sql, Binder binder) {
        return listar(sql, binder).stream().findFirst();
    }

    private List<Categoria> listar(String sql, Binder binder) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Categoria> categorias = new ArrayList<>();
                while (resultSet.next()) categorias.add(mapear(resultSet));
                return categorias;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar as categorias.", exception);
        }
    }

    private Categoria mapear(ResultSet resultSet) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setId(resultSet.getLong("id"));
        categoria.setNome(resultSet.getString("nome"));
        categoria.setAtiva(true);
        categoria.setCriadoEm(LocalDateTime.parse(resultSet.getString("criado_em")));
        categoria.setAtualizadoEm(LocalDateTime.parse(resultSet.getString("atualizado_em")));
        return categoria;
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
