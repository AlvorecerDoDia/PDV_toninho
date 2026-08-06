package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.EntityNotFoundException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.repository.UsuarioRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persiste usuarios mantendo compatibilidade com bancos antigos. */
public final class SQLiteUsuarioRepository implements UsuarioRepository {
    private static final String PERFIL_LEGADO = "OPERADOR";
    private final Database database;

    public SQLiteUsuarioRepository(Database database) {
        this.database = database;
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        String sql = """
                INSERT INTO usuario (
                    nome, login, senha_hash, perfil, ativo, alterar_senha, criado_em, atualizado_em
                ) VALUES (?, ?, ?, ?, ?, 0, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, usuario.getNome());
            statement.setString(2, usuario.getLogin());
            statement.setString(3, usuario.getSenhaHash());
            statement.setString(4, PERFIL_LEGADO);
            statement.setBoolean(5, usuario.isAtivo());
            statement.setString(6, usuario.getCriadoEm().toString());
            statement.setString(7, usuario.getAtualizadoEm().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) usuario.setId(keys.getLong(1));
            }
            return usuario;
        } catch (SQLException exception) {
            throw traduzir(exception);
        }
    }

    @Override
    public void atualizar(Usuario usuario) {
        String sql = """
                UPDATE usuario SET nome = ?, login = ?, senha_hash = ?,
                    perfil = ?, ativo = ?, alterar_senha = 0, atualizado_em = ?
                WHERE id = ?
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuario.getNome());
            statement.setString(2, usuario.getLogin());
            statement.setString(3, usuario.getSenhaHash());
            statement.setString(4, PERFIL_LEGADO);
            statement.setBoolean(5, usuario.isAtivo());
            statement.setString(6, usuario.getAtualizadoEm().toString());
            statement.setLong(7, usuario.getId());
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Usuário não encontrado.");
            }
        } catch (SQLException exception) {
            throw traduzir(exception);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(long id) {
        return um("SELECT * FROM usuario WHERE id = ?", statement -> statement.setLong(1, id));
    }

    @Override
    public Optional<Usuario> buscarPorLogin(String login) {
        return um("SELECT * FROM usuario WHERE login = ?", statement -> statement.setString(1, login));
    }

    @Override
    public List<Usuario> listar() {
        return listar("SELECT * FROM usuario ORDER BY nome", null);
    }

    @Override
    public long contar() {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM usuario")) {
            return resultSet.next() ? resultSet.getLong(1) : 0;
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível contar os usuários.", exception);
        }
    }

    private Optional<Usuario> um(String sql, Binder binder) {
        return listar(sql, binder).stream().findFirst();
    }

    private List<Usuario> listar(String sql, Binder binder) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Usuario> usuarios = new ArrayList<>();
                while (resultSet.next()) usuarios.add(mapear(resultSet));
                return usuarios;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar os usuários.", exception);
        }
    }

    private Usuario mapear(ResultSet resultSet) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(resultSet.getLong("id"));
        usuario.setNome(resultSet.getString("nome"));
        usuario.setLogin(resultSet.getString("login"));
        usuario.setSenhaHash(resultSet.getString("senha_hash"));
        usuario.setAtivo(resultSet.getBoolean("ativo"));
        usuario.setCriadoEm(LocalDateTime.parse(resultSet.getString("criado_em")));
        usuario.setAtualizadoEm(LocalDateTime.parse(resultSet.getString("atualizado_em")));
        return usuario;
    }

    private RuntimeException traduzir(SQLException exception) {
        if (exception.getErrorCode() == 19) {
            return new ValidationException("Já existe um usuário com esse login.");
        }
        return new DatabaseException("Não foi possível salvar o usuário.", exception);
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
