package br.com.loja.pdv.infrastructure.database;

import br.com.loja.pdv.exception.DatabaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    @TempDir
    Path tempDirectory;

    @Test
    void deveCriarDiretorioInexistenteEConfigurarConexao() throws Exception {
        Path databaseFile = tempDirectory.resolve("inexistente").resolve("teste.db");
        Database database = new Database(databaseFile);

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            assertEquals(1, pragmaInt(statement, "foreign_keys"));
            assertEquals(5000, pragmaInt(statement, "busy_timeout"));
            assertEquals("wal", pragmaText(statement, "journal_mode"));
        }

        assertTrue(Files.exists(databaseFile));
    }

    @Test
    void deveInformarErroQuandoDiretorioNaoPodeSerCriado() throws IOException {
        Path regularFile = tempDirectory.resolve("arquivo");
        Files.writeString(regularFile, "conteúdo");
        Database database = new Database(regularFile.resolve("teste.db"));

        DatabaseException exception = assertThrows(
                DatabaseException.class,
                database::getConnection
        );

        assertTrue(exception.getMessage().contains("pasta de dados"));
    }

    private int pragmaInt(Statement statement, String pragma) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("PRAGMA " + pragma)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private String pragmaText(Statement statement, String pragma) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("PRAGMA " + pragma)) {
            assertTrue(resultSet.next());
            return resultSet.getString(1);
        }
    }
}
