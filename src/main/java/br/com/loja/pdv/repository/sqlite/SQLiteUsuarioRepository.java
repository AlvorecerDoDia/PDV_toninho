package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.enums.PerfilUsuario;
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

/** Persiste usuarios sem nunca armazenar a senha original. */
public final class SQLiteUsuarioRepository implements UsuarioRepository {
    private final Database database;

    /** Recebe a configuracao de banco usada para abrir conexoes em cada operacao. */
    public SQLiteUsuarioRepository(Database database) {
        this.database = database;
    }

    /** Insere usuario com senha ja transformada em hash. */
    @Override
    public Usuario salvar(Usuario usuario) {
        String sql = """
                INSERT INTO usuario (
                    nome, login, senha_hash, perfil, ativo, alterar_senha, criado_em, atualizado_em
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, usuario);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) usuario.setId(keys.getLong(1));
            }
            return usuario;
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    /** Atualiza dados, perfil, status e credencial do usuario. */
    @Override
    public void atualizar(Usuario usuario) {
        String sql = """
                UPDATE usuario SET nome = ?, login = ?, senha_hash = ?, perfil = ?,
                    ativo = ?, alterar_senha = ?, criado_em = ?, atualizado_em = ?
                WHERE id = ?
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, usuario);
            statement.setLong(9, usuario.getId());
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Usuário não encontrado.");
            }
        } catch (SQLException exception) {
            throw translate(exception);
        }
    }

    @Override public Optional<Usuario> buscarPorId(long id) {
        return one("SELECT * FROM usuario WHERE id = ?", statement -> statement.setLong(1, id));
    }
    @Override public Optional<Usuario> buscarPorLogin(String login) {
        return one("SELECT * FROM usuario WHERE login = ?", statement -> statement.setString(1, login));
    }
    @Override public List<Usuario> listar() {
        return list("SELECT * FROM usuario ORDER BY nome", null);
    }
    @Override public long contar() {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM usuario")) {
            return resultSet.next() ? resultSet.getLong(1) : 0;
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível contar os usuários.", exception);
        }
    }

    /** Executa consulta que retorna no maximo um usuario. */
    private Optional<Usuario> one(String sql, Binder binder) {
        return list(sql, binder).stream().findFirst();
    }

    /** Executa consulta de varios usuarios. */
    private List<Usuario> list(String sql, Binder binder) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Usuario> result = new ArrayList<>();
                while (resultSet.next()) result.add(map(resultSet));
                return result;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar os usuários.", exception);
        }
    }

    /** Preenche os parametros comuns do usuario. */
    private void bind(PreparedStatement statement, Usuario usuario) throws SQLException {
        statement.setString(1, usuario.getNome());
        statement.setString(2, usuario.getLogin());
        statement.setString(3, usuario.getSenhaHash());
        statement.setString(4, usuario.getPerfil().name());
        statement.setBoolean(5, usuario.isAtivo());
        statement.setBoolean(6, usuario.isAlterarSenha());
        statement.setString(7, usuario.getCriadoEm().toString());
        statement.setString(8, usuario.getAtualizadoEm().toString());
    }

    /** Converte uma linha JDBC em Usuario. */
    private Usuario map(ResultSet resultSet) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(resultSet.getLong("id"));
        usuario.setNome(resultSet.getString("nome"));
        usuario.setLogin(resultSet.getString("login"));
        usuario.setSenhaHash(resultSet.getString("senha_hash"));
        usuario.setPerfil(PerfilUsuario.valueOf(resultSet.getString("perfil")));
        usuario.setAtivo(resultSet.getBoolean("ativo"));
        usuario.setAlterarSenha(resultSet.getBoolean("alterar_senha"));
        usuario.setCriadoEm(LocalDateTime.parse(resultSet.getString("criado_em")));
        usuario.setAtualizadoEm(LocalDateTime.parse(resultSet.getString("atualizado_em")));
        return usuario;
    }

    /** Traduz login duplicado e outras falhas do banco. */
    private RuntimeException translate(SQLException exception) {
        if (exception.getErrorCode() == 19) {
            return new ValidationException("Já existe um usuário com esse login.");
        }
        return new DatabaseException("Não foi possível salvar o usuário.", exception);
    }

    /** Configura os parametros de uma consulta reutilizavel. */
    @FunctionalInterface
    private interface Binder {
        /** Preenche os parametros antes da execucao. */
        void bind(PreparedStatement statement) throws SQLException;
    }
}
