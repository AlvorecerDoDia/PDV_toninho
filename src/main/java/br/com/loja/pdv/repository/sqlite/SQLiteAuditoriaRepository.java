package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.model.RegistroAuditoria;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.repository.AuditoriaRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class SQLiteAuditoriaRepository implements AuditoriaRepository {
    private final Database database;

    public SQLiteAuditoriaRepository(Database database) {
        this.database = database;
    }

    @Override
    public RegistroAuditoria salvar(RegistroAuditoria registro) {
        String sql = """
                INSERT INTO auditoria (
                    usuario_id, acao, entidade, entidade_id,
                    valores_anteriores, valores_novos, criado_em
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableLong(statement, 1, registro.getUsuarioId());
            statement.setString(2, registro.getAcao());
            statement.setString(3, registro.getEntidade());
            setNullableLong(statement, 4, registro.getEntidadeId());
            statement.setString(5, registro.getValoresAnteriores());
            statement.setString(6, registro.getValoresNovos());
            statement.setString(7, registro.getCriadoEm().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) registro.setId(keys.getLong(1));
            }
            return registro;
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível registrar a auditoria.", exception);
        }
    }

    @Override
    public List<RegistroAuditoria> listarRecentes(int limite) {
        String sql = "SELECT * FROM auditoria ORDER BY criado_em DESC, id DESC LIMIT ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limite);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RegistroAuditoria> registros = new ArrayList<>();
                while (resultSet.next()) registros.add(map(resultSet));
                return registros;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar a auditoria.", exception);
        }
    }

    private RegistroAuditoria map(ResultSet resultSet) throws SQLException {
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setId(resultSet.getLong("id"));
        registro.setUsuarioId(nullableLong(resultSet, "usuario_id"));
        registro.setAcao(resultSet.getString("acao"));
        registro.setEntidade(resultSet.getString("entidade"));
        registro.setEntidadeId(nullableLong(resultSet, "entidade_id"));
        registro.setValoresAnteriores(resultSet.getString("valores_anteriores"));
        registro.setValoresNovos(resultSet.getString("valores_novos"));
        registro.setCriadoEm(LocalDateTime.parse(resultSet.getString("criado_em")));
        return registro;
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
}
