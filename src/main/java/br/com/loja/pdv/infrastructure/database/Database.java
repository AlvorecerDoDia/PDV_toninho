package br.com.loja.pdv.infrastructure.database;

import br.com.loja.pdv.config.AppPaths;
import br.com.loja.pdv.exception.DatabaseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Abre conexoes SQLite ja configuradas com integridade e espera por bloqueios. */
public final class Database {

    private final Path databaseFile;
    private final String url;

    /** Normaliza e guarda o caminho do arquivo SQLite que sera aberto. */
    public Database(Path databaseFile) {
        if (databaseFile == null) {
            throw new IllegalArgumentException("O arquivo do banco é obrigatório.");
        }
        this.databaseFile = databaseFile.toAbsolutePath().normalize();
        this.url = "jdbc:sqlite:" + this.databaseFile;
    }

    /** Cria a configuracao padrao apontando para a pasta de dados da aplicacao. */
    public static Database local() {
        return new Database(AppPaths.databaseFile());
    }

    /** Exponibiliza o caminho normalizado usado por backup e diagnostico. */
    public Path getDatabaseFile() {
        return databaseFile;
    }

    /** Cria uma conexao nova, prepara a pasta e aplica os pragmas obrigatorios. */
    public Connection getConnection() throws SQLException {
        createDataDirectory();
        Connection connection = DriverManager.getConnection(url);

        try {
            configure(connection);
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    /** Cria a pasta pai antes que o SQLite tente abrir o arquivo. */
    private void createDataDirectory() {
        Path directory = databaseFile.getParent();
        if (directory == null) {
            return;
        }
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new DatabaseException(
                    "Não foi possível criar a pasta de dados.",
                    exception
            );
        }
    }

    /** Ativa integridade referencial, espera por bloqueios e modo WAL. */
    private void configure(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
        }
    }
}
