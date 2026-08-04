package br.com.loja.pdv.infrastructure.database;

import br.com.loja.pdv.exception.DatabaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa um componente de infraestrutura com recursos controlados pelo teste. */
class DatabaseMigratorTest {

    @TempDir
    Path tempDirectory;

    /** Verifica o cenario: deve criar banco vazio eexecutar migracoes. */
    @Test
    void deveCriarBancoVazioEExecutarMigracoes() throws Exception {
        Database database = database("vazio.db");

        new DatabaseInitializer(database).initialize();

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            assertTrue(tableExists(statement, "schema_version"));
            assertTrue(tableExists(statement, "produto"));
            assertTrue(tableExists(statement, "caixa"));
            assertTrue(tableExists(statement, "movimentacao_caixa"));
            assertTrue(tableExists(statement, "venda"));
            assertTrue(tableExists(statement, "item_venda"));
            assertTrue(tableExists(statement, "pagamento"));
            assertTrue(tableExists(statement, "auditoria"));
            assertTrue(tableExists(statement, "categoria"));
            assertEquals(5, count(statement, "categoria"));
            assertEquals(7, count(statement, "schema_version"));
        }
    }

    /** Verifica o cenario: deve reinicializar sem duplicar migracoes. */
    @Test
    void deveReinicializarSemDuplicarMigracoes() throws Exception {
        Database database = database("reinicializacao.db");
        DatabaseInitializer initializer = new DatabaseInitializer(database);

        initializer.initialize();
        initializer.initialize();

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            assertEquals(7, count(statement, "schema_version"));
        }
    }

    /** Verifica o cenario: deve aplicar restricoes check da tabela produto. */
    @Test
    void deveAplicarRestricoesCheckDaTabelaProduto() throws Exception {
        Database database = database("restricoes.db");
        new DatabaseInitializer(database).initialize();

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            String invalidInsert = """
                    INSERT INTO produto (
                        nome, preco_custo_centavos, preco_venda_centavos,
                        quantidade_estoque, estoque_minimo, ativo,
                        criado_em, atualizado_em
                    ) VALUES ('Inválido', -1, 100, 0, 0, 1, '2026-01-01', '2026-01-01')
                    """;
            assertThrows(SQLException.class, () -> statement.executeUpdate(invalidInsert));
        }
    }

    /** Verifica o cenario: deve manter chaves estrangeiras ativadas apos migracao. */
    @Test
    void deveManterChavesEstrangeirasAtivadasAposMigracao() throws Exception {
        Database database = database("foreign-key.db");
        new DatabaseInitializer(database).initialize();

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_keys")) {
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1));
        }
    }

    /** Verifica o cenario: deve recusar versao de esquema desconhecida. */
    @Test
    void deveRecusarVersaoDeEsquemaDesconhecida() throws Exception {
        Database database = database("versao-desconhecida.db");
        new DatabaseInitializer(database).initialize();

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO schema_version (version, description, applied_at)
                    VALUES (999, 'versão futura', '2026-01-01T00:00:00')
                    """);
        }

        assertThrows(
                DatabaseException.class,
                () -> new DatabaseInitializer(database).initialize()
        );
    }

    private Database database(String filename) {
        return new Database(tempDirectory.resolve("data").resolve(filename));
    }

    private boolean tableExists(Statement statement, String table) throws SQLException {
        String query = """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'table' AND name = '%s'
                """.formatted(table);
        try (ResultSet resultSet = statement.executeQuery(query)) {
            return resultSet.next() && resultSet.getInt(1) == 1;
        }
    }

    private int count(Statement statement, String table) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }
}
