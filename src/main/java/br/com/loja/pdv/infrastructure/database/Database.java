package br.com.loja.pdv.infrastructure.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path DATABASE_FILE =
            DATA_DIRECTORY.resolve("pdv.db");

    private static final String URL =
            "jdbc:sqlite:" + DATABASE_FILE.toAbsolutePath();

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        createDataDirectory();

        Connection connection = DriverManager.getConnection(URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }

        return connection;
    }

    private static void createDataDirectory() {
        try {
            Files.createDirectories(DATA_DIRECTORY);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível criar a pasta de dados.",
                    exception
            );
        }
    }
}