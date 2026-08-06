package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.enums.StatusCaixa;
import br.com.loja.pdv.domain.enums.TipoMovimentacaoCaixa;
import br.com.loja.pdv.domain.model.Caixa;
import br.com.loja.pdv.domain.model.MovimentacaoCaixa;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.EntityNotFoundException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.repository.CaixaRepository;
import br.com.loja.pdv.util.MoneyUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persiste caixa e movimentacoes preservando consistencia transacional. */
public final class SQLiteCaixaRepository implements CaixaRepository {
    private final Database database;

    /** Recebe a configuracao de banco usada para abrir conexoes em cada operacao. */
    public SQLiteCaixaRepository(Database database) {
        this.database = database;
    }

    /** Insere o caixa e sua movimentacao de abertura na mesma transacao. */
    @Override
    public Caixa abrir(Caixa caixa, MovimentacaoCaixa abertura) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // A abertura e seu lancamento inicial sao indivisiveis.
                insertCashRegister(connection, caixa);
                abertura.setCaixaId(caixa.getId());
                insertMovement(connection, abertura);
                connection.commit();
                return caixa;
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                if (isUniqueConstraint(exception)) {
                    throw new ValidationException("O usuário já possui um caixa aberto.");
                }
                throw translate("Não foi possível abrir o caixa.", exception);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível acessar o caixa.", exception);
        }
    }

    /** Consulta um caixa pelo identificador. */
    @Override
    public Optional<Caixa> buscarPorId(long id) {
        return findOne("SELECT * FROM caixa WHERE id = ?", id);
    }

    /** Localiza o caixa aberto do operador informado. */
    @Override
    public Optional<Caixa> buscarAbertoPorUsuario(long usuarioId) {
        return findOne("""
                SELECT * FROM caixa
                WHERE usuario_id = ? AND status = 'ABERTO'
                ORDER BY id DESC LIMIT 1
                """, usuarioId);
    }

    /** Atualiza os valores finais e o status dentro de uma transacao. */
    @Override
    public Caixa fechar(long caixaId, BigDecimal valorContado, LocalDateTime fechadoEm) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // O valor esperado e congelado no mesmo instante em que o caixa e fechado.
                ensureOpen(connection, caixaId);
                BigDecimal expected = expected(connection, caixaId);
                BigDecimal difference = valorContado.subtract(expected);
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE caixa SET status = 'FECHADO', valor_esperado_centavos = ?,
                            valor_contado_centavos = ?, diferenca_centavos = ?, fechado_em = ?
                        WHERE id = ? AND status = 'ABERTO'
                        """)) {
                    statement.setLong(1, MoneyUtils.toCents(expected));
                    statement.setLong(2, MoneyUtils.toCents(valorContado));
                    statement.setLong(3, MoneyUtils.toCents(difference));
                    statement.setString(4, fechadoEm.toString());
                    statement.setLong(5, caixaId);
                    statement.executeUpdate();
                }
                connection.commit();
                return buscarPorId(caixaId).orElseThrow();
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                throw translate("Não foi possível fechar o caixa.", exception);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível acessar o caixa.", exception);
        }
    }

    /** Calcula no banco o dinheiro esperado a partir das movimentacoes. */
    @Override
    public BigDecimal buscarDinheiroEsperado(long caixaId) {
        try (Connection connection = database.getConnection()) {
            ensureExists(connection, caixaId);
            return expected(connection, caixaId);
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível calcular o caixa.", exception);
        }
    }

    /** Retorna o historico financeiro do caixa em ordem cronologica. */
    @Override
    public List<MovimentacaoCaixa> listarMovimentacoes(long caixaId) {
        String sql = """
                SELECT * FROM movimentacao_caixa
                WHERE caixa_id = ? ORDER BY criado_em DESC, id DESC
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, caixaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MovimentacaoCaixa> result = new ArrayList<>();
                while (resultSet.next()) result.add(mapMovement(resultSet));
                return result;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar as movimentações.", exception);
        }
    }

    /** Executa uma consulta de caixa que pode nao retornar resultado. */
    private Optional<Caixa> findOne(String sql, long parameter) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapCashRegister(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar o caixa.", exception);
        }
    }

    /** Insere o cabecalho do caixa e recupera sua chave gerada. */
    private void insertCashRegister(Connection connection, Caixa caixa) throws SQLException {
        String sql = """
                INSERT INTO caixa (usuario_id, status, valor_abertura_centavos, aberto_em)
                VALUES (?, 'ABERTO', ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, caixa.getUsuarioId());
            statement.setLong(2, MoneyUtils.toCents(caixa.getValorAbertura()));
            statement.setString(3, caixa.getAbertoEm().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("ID do caixa não retornado.");
                caixa.setId(keys.getLong(1));
            }
        }
    }

    /** Persiste uma movimentacao financeira associada ao caixa. */
    private void insertMovement(Connection connection, MovimentacaoCaixa movement)
            throws SQLException {
        String sql = """
                INSERT INTO movimentacao_caixa (
                    caixa_id, usuario_id, tipo, valor_centavos, motivo, criado_em
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, movement.getCaixaId());
            statement.setLong(2, movement.getUsuarioId());
            statement.setString(3, movement.getTipo().name());
            statement.setLong(4, MoneyUtils.toCents(movement.getValor()));
            statement.setString(5, movement.getMotivo());
            statement.setString(6, movement.getCriadoEm().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) movement.setId(keys.getLong(1));
            }
        }
    }

    /** Bloqueia alteracoes em caixa fechado ou inexistente. */
    private void ensureOpen(Connection connection, long caixaId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT status FROM caixa WHERE id = ?")) {
            statement.setLong(1, caixaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new EntityNotFoundException("Caixa não encontrado.");
                if (!StatusCaixa.ABERTO.name().equals(resultSet.getString(1))) {
                    throw new ValidationException("O caixa está fechado.");
                }
            }
        }
    }

    /** Confirma que o caixa informado existe antes de consultar agregados. */
    private void ensureExists(Connection connection, long caixaId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM caixa WHERE id = ?")) {
            statement.setLong(1, caixaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new EntityNotFoundException("Caixa não encontrado.");
            }
        }
    }

    /** Soma no SQL apenas os tipos que afetam o dinheiro fisico. */
    private BigDecimal expected(Connection connection, long caixaId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(CASE
                    WHEN tipo IN ('ABERTURA', 'VENDA_DINHEIRO', 'SUPRIMENTO')
                    THEN valor_centavos ELSE -valor_centavos END), 0)
                FROM movimentacao_caixa WHERE caixa_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, caixaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return MoneyUtils.fromCents(resultSet.getLong(1));
            }
        }
    }

    /** Converte a linha JDBC em Caixa. */
    private Caixa mapCashRegister(ResultSet resultSet) throws SQLException {
        Caixa caixa = new Caixa();
        caixa.setId(resultSet.getLong("id"));
        caixa.setUsuarioId(resultSet.getLong("usuario_id"));
        caixa.setStatus(StatusCaixa.valueOf(resultSet.getString("status")));
        caixa.setValorAbertura(MoneyUtils.fromCents(
                resultSet.getLong("valor_abertura_centavos")));
        caixa.setValorEsperado(nullableMoney(resultSet, "valor_esperado_centavos"));
        caixa.setValorContado(nullableMoney(resultSet, "valor_contado_centavos"));
        caixa.setDiferenca(nullableMoney(resultSet, "diferenca_centavos"));
        caixa.setAbertoEm(LocalDateTime.parse(resultSet.getString("aberto_em")));
        String closed = resultSet.getString("fechado_em");
        caixa.setFechadoEm(closed == null ? null : LocalDateTime.parse(closed));
        return caixa;
    }

    /** Converte a linha JDBC em MovimentacaoCaixa. */
    private MovimentacaoCaixa mapMovement(ResultSet resultSet) throws SQLException {
        MovimentacaoCaixa movement = new MovimentacaoCaixa();
        movement.setId(resultSet.getLong("id"));
        movement.setCaixaId(resultSet.getLong("caixa_id"));
        movement.setUsuarioId(resultSet.getLong("usuario_id"));
        movement.setTipo(TipoMovimentacaoCaixa.valueOf(resultSet.getString("tipo")));
        movement.setValor(MoneyUtils.fromCents(resultSet.getLong("valor_centavos")));
        movement.setMotivo(resultSet.getString("motivo"));
        movement.setCriadoEm(LocalDateTime.parse(resultSet.getString("criado_em")));
        return movement;
    }

    /** Le centavos opcionais e converte para BigDecimal. */
    private BigDecimal nullableMoney(ResultSet resultSet, String column) throws SQLException {
        long cents = resultSet.getLong(column);
        return resultSet.wasNull() ? null : MoneyUtils.fromCents(cents);
    }

    /** Traduz restricoes conhecidas do SQLite em excecoes de negocio. */
    private RuntimeException translate(String message, Exception exception) {
        if (exception instanceof RuntimeException runtimeException) return runtimeException;
        return new DatabaseException(message, exception);
    }

    /** Identifica a violacao que indica caixa ja aberto para o operador. */
    private boolean isUniqueConstraint(Exception exception) {
        return exception instanceof SQLException sql
                && sql.getMessage() != null
                && sql.getMessage().contains("UNIQUE constraint failed");
    }

    /** Desfaz a transacao e preserva falhas ocorridas durante o rollback. */
    private void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            cause.addSuppressed(exception);
        }
    }
}
