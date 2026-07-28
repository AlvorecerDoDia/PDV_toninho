package br.com.loja.pdv.infrastructure.database;

import br.com.loja.pdv.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class DatabaseMigrator {

    private static final String MIGRATION_DIRECTORY =
            "/br/com/loja/pdv/database/migration/";

    private static final List<Migration> MIGRATIONS = List.of(
            new Migration(1, "cria tabela produto", "V001__cria_tabela_produto.sql"),
            new Migration(2, "cria movimentacao estoque", "V002__cria_movimentacao_estoque.sql"),
            new Migration(3, "cria tabela usuario", "V003__cria_usuario.sql"),
            new Migration(4, "cria caixa e movimentacoes", "V004__cria_caixa.sql"),
            new Migration(5, "cria venda itens e pagamentos", "V005__cria_venda.sql"),
            new Migration(6, "cria auditoria", "V006__cria_auditoria.sql")
    );

    private final Database database;

    public DatabaseMigrator(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("O banco é obrigatório.");
        }
        this.database = database;
    }

    public void migrate() {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                createSchemaVersionTable(connection);
                Set<Integer> appliedVersions = readAppliedVersions(connection);
                validateAppliedVersions(appliedVersions);

                for (Migration migration : MIGRATIONS) {
                    if (!appliedVersions.contains(migration.version())) {
                        applyMigration(connection, migration);
                    }
                }
                connection.commit();
            } catch (Exception exception) {
                rollback(connection, exception);
                throw asDatabaseException(exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível atualizar o banco de dados.", exception);
        }
    }

    private void createSchemaVersionTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER PRIMARY KEY,
                    description TEXT NOT NULL,
                    applied_at TEXT NOT NULL
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Set<Integer> readAppliedVersions(Connection connection) throws SQLException {
        Set<Integer> versions = new TreeSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT version FROM schema_version ORDER BY version")) {
            while (resultSet.next()) {
                versions.add(resultSet.getInt("version"));
            }
        }
        return versions;
    }

    private void validateAppliedVersions(Set<Integer> appliedVersions) {
        Set<Integer> knownVersions = new TreeSet<>();
        MIGRATIONS.forEach(migration -> knownVersions.add(migration.version()));
        if (!knownVersions.containsAll(appliedVersions)) {
            throw new DatabaseException(
                    "O banco possui uma versão de esquema desconhecida pela aplicação."
            );
        }
    }

    private void applyMigration(Connection connection, Migration migration)
            throws SQLException, IOException {
        for (String command : splitCommands(readScript(migration.resource()))) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(command);
            }
        }

        String insert = """
                INSERT INTO schema_version (version, description, applied_at)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setInt(1, migration.version());
            statement.setString(2, migration.description());
            statement.setString(3, LocalDateTime.now().toString());
            statement.executeUpdate();
        }
    }

    private String readScript(String resource) throws IOException {
        String path = MIGRATION_DIRECTORY + resource;
        try (InputStream input = DatabaseMigrator.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Migração não encontrada: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<String> splitCommands(String script) {
        String commands = script.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("--"))
                .reduce("", (result, line) -> result + line + System.lineSeparator());

        return Arrays.stream(commands.split(";"))
                .map(String::strip)
                .filter(command -> !command.isEmpty())
                .toList();
    }

    private void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }

    private DatabaseException asDatabaseException(Exception exception) {
        if (exception instanceof DatabaseException databaseException) {
            return databaseException;
        }
        return new DatabaseException("Não foi possível executar as migrações do banco.", exception);
    }

    private record Migration(int version, String description, String resource) {
    }
}
