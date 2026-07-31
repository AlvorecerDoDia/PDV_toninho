package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.enums.TipoMovimentacaoEstoque;
import br.com.loja.pdv.domain.model.MovimentacaoEstoque;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.EntityNotFoundException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.repository.EstoqueRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Atualiza saldo e historico de estoque na mesma transacao SQLite. */
public final class SQLiteEstoqueRepository implements EstoqueRepository {

    private final Database database;

    public SQLiteEstoqueRepository(Database database) {
        this.database = database;
    }

    @Override
    public MovimentacaoEstoque registrar(MovimentacaoEstoque movimentacao) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // O saldo do produto e seu historico devem sempre avancar juntos.
                int previous = readBalance(connection, movimentacao.getProdutoId());
                int next = movimentacao.getTipo().apply(previous, movimentacao.getQuantidade());
                if (next < 0) {
                    throw new ValidationException("A movimentação deixaria o estoque negativo.");
                }
                movimentacao.setQuantidadeAnterior(previous);
                movimentacao.setQuantidadePosterior(next);
                updateBalance(connection, movimentacao.getProdutoId(), next, movimentacao.getCriadoEm());
                insertMovement(connection, movimentacao);
                connection.commit();
                return movimentacao;
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new DatabaseException("Não foi possível registrar a movimentação.", exception);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível acessar o estoque.", exception);
        }
    }

    @Override
    public int buscarSaldo(long produtoId) {
        try (Connection connection = database.getConnection()) {
            return readBalance(connection, produtoId);
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar o estoque.", exception);
        }
    }

    @Override
    public List<MovimentacaoEstoque> listar(
            long produtoId, LocalDateTime inicio, LocalDateTime fim) {
        String sql = """
                SELECT * FROM movimentacao_estoque
                WHERE produto_id = ? AND criado_em >= ? AND criado_em <= ?
                ORDER BY criado_em DESC, id DESC
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, produtoId);
            statement.setString(2, inicio.toString());
            statement.setString(3, fim.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MovimentacaoEstoque> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(map(resultSet));
                }
                return result;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar o histórico de estoque.", exception);
        }
    }

    private int readBalance(Connection connection, long produtoId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT quantidade_estoque FROM produto WHERE id = ?")) {
            statement.setLong(1, produtoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new EntityNotFoundException("Produto não encontrado.");
                }
                return resultSet.getInt(1);
            }
        }
    }

    private void updateBalance(
            Connection connection, long produtoId, int balance, LocalDateTime timestamp)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE produto SET quantidade_estoque = ?, atualizado_em = ? WHERE id = ?
                """)) {
            statement.setInt(1, balance);
            statement.setString(2, timestamp.toString());
            statement.setLong(3, produtoId);
            statement.executeUpdate();
        }
    }

    private void insertMovement(Connection connection, MovimentacaoEstoque movement)
            throws SQLException {
        String sql = """
                INSERT INTO movimentacao_estoque (
                    produto_id, tipo, quantidade, quantidade_anterior,
                    quantidade_posterior, motivo, usuario_id, venda_id, criado_em
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, movement.getProdutoId());
            statement.setString(2, movement.getTipo().name());
            statement.setInt(3, movement.getQuantidade());
            statement.setInt(4, movement.getQuantidadeAnterior());
            statement.setInt(5, movement.getQuantidadePosterior());
            statement.setString(6, movement.getMotivo());
            setNullableLong(statement, 7, movement.getUsuarioId());
            setNullableLong(statement, 8, movement.getVendaId());
            statement.setString(9, movement.getCriadoEm().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) movement.setId(keys.getLong(1));
            }
        }
    }

    private MovimentacaoEstoque map(ResultSet resultSet) throws SQLException {
        MovimentacaoEstoque movement = new MovimentacaoEstoque();
        movement.setId(resultSet.getLong("id"));
        movement.setProdutoId(resultSet.getLong("produto_id"));
        movement.setTipo(TipoMovimentacaoEstoque.valueOf(resultSet.getString("tipo")));
        movement.setQuantidade(resultSet.getInt("quantidade"));
        movement.setQuantidadeAnterior(resultSet.getInt("quantidade_anterior"));
        movement.setQuantidadePosterior(resultSet.getInt("quantidade_posterior"));
        movement.setMotivo(resultSet.getString("motivo"));
        movement.setUsuarioId(nullableLong(resultSet, "usuario_id"));
        movement.setVendaId(nullableLong(resultSet, "venda_id"));
        movement.setCriadoEm(LocalDateTime.parse(resultSet.getString("criado_em")));
        return movement;
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.BIGINT);
        else statement.setLong(index, value);
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            cause.addSuppressed(exception);
        }
    }
}
