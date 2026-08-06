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

/** Descobre e aplica scripts versionados uma unica vez, dentro de transacao. */
public final class DatabaseMigrator {

    private static final String MIGRATION_DIRECTORY =
            "/br/com/loja/pdv/database/migration/";

    private static final List<Migration> MIGRATIONS = List.of(
            new Migration(1, "cria tabela produto", "V001__cria_tabela_produto.sql"),
            new Migration(2, "cria movimentacao estoque", "V002__cria_movimentacao_estoque.sql"),
            new Migration(3, "cria tabela usuario", "V003__cria_usuario.sql"),
            new Migration(4, "cria caixa e movimentacoes", "V004__cria_caixa.sql"),
            new Migration(5, "cria venda itens e pagamentos", "V005__cria_venda.sql"),
            new Migration(6, "cria auditoria", "V006__cria_auditoria.sql"),
            new Migration(7, "cria categoria de produto", "V007__cria_categoria_produto.sql"),
            new Migration(8, "registra categoria historica da venda",
                    "V008__registra_categoria_historica_item_venda.sql")
    );

    private final Database database;

    /** Recebe o banco no qual as migracoes serao aplicadas. */
    public DatabaseMigrator(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("O banco é obrigatório.");
        }
        this.database = database;
    }

    /** Executa migracoes ainda nao aplicadas dentro de uma unica transacao. */
    public void migrate() {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // A tabela de controle e cada script sao confirmados em conjunto:
                // uma versao so aparece como aplicada quando todo o SQL termina.
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

    /** Garante a existencia da tabela que controla as versoes aplicadas. */
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

    /** Le do banco todas as versoes executadas anteriormente. */
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

    /** Interrompe a inicializacao quando o banco possui versao desconhecida. */
    private void validateAppliedVersions(Set<Integer> appliedVersions) {
        // Impede abrir com codigo antigo um banco ja alterado por uma versao futura.
        Set<Integer> knownVersions = new TreeSet<>();
        MIGRATIONS.forEach(migration -> knownVersions.add(migration.version()));
        if (!knownVersions.containsAll(appliedVersions)) {
            throw new DatabaseException(
                    "O banco possui uma versão de esquema desconhecida pela aplicação."
            );
        }
    }

    /** Executa o script e registra sua versao somente depois do sucesso. */
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

    /** Carrega um arquivo SQL empacotado nos recursos da aplicacao. */
    private String readScript(String resource) throws IOException {
        String path = MIGRATION_DIRECTORY + resource;
        try (InputStream input = DatabaseMigrator.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Migração não encontrada: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Remove comentarios SQL e separa o script em comandos individuais. */
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

    /** Tenta desfazer a transacao sem esconder a causa original. */
    private void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }

    /** Preserva erros de banco existentes ou envolve outras excecoes. */
    private DatabaseException asDatabaseException(Exception exception) {
        if (exception instanceof DatabaseException databaseException) {
            return databaseException;
        }
        return new DatabaseException("Não foi possível executar as migrações do banco.", exception);
    }

    /** Descreve uma versao e o arquivo SQL correspondente. */
    private record Migration(int version, String description, String resource) {
    }
}
