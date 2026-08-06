package br.com.loja.pdv.infrastructure.backup;

import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.infrastructure.database.Database;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** Cria backups automaticos do banco e remove copias antigas. */
public final class GerenciadorBackup {
    private static final DateTimeFormatter NAME_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private final Database database;
    private final Path backupDirectory;
    private final Clock clock;

    public GerenciadorBackup(Database database, Path backupDirectory) {
        this(database, backupDirectory, Clock.systemDefaultZone());
    }

    GerenciadorBackup(Database database, Path backupDirectory, Clock clock) {
        this.database = database;
        this.backupDirectory = backupDirectory.toAbsolutePath().normalize();
        this.clock = clock;
    }

    public Path criarAutomatico() {
        try {
            Files.createDirectories(backupDirectory);
            Path destino = backupDirectory.resolve(
                    "automatico-" + LocalDateTime.now(clock).format(NAME_DATE) + ".db");
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("VACUUM INTO ?")) {
                statement.setString(1, destino.toString());
                statement.execute();
            }
            if (!Files.isRegularFile(destino) || Files.size(destino) == 0) {
                throw new DatabaseException("O backup não foi criado corretamente.");
            }
            validar(destino);
            return destino;
        } catch (SQLException | IOException exception) {
            throw new DatabaseException("Não foi possível criar o backup.", exception);
        }
    }

    public void aplicarRetencao(int quantidade) {
        if (quantidade < 1) throw new IllegalArgumentException("Retenção inválida.");
        try {
            if (!Files.isDirectory(backupDirectory)) return;
            List<Path> arquivos;
            try (var stream = Files.list(backupDirectory)) {
                arquivos = stream
                        .filter(path -> path.getFileName().toString().endsWith(".db"))
                        .sorted(Comparator.comparing(this::ultimaAlteracao).reversed())
                        .toList();
            }
            for (int indice = quantidade; indice < arquivos.size(); indice++) {
                Files.deleteIfExists(arquivos.get(indice));
            }
        } catch (IOException exception) {
            throw new DatabaseException("Não foi possível limpar os backups antigos.", exception);
        }
    }

    private void validar(Path arquivo) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA quick_check")) {
            if (!resultSet.next() || !"ok".equalsIgnoreCase(resultSet.getString(1))) {
                throw new DatabaseException("O backup criado está corrompido.");
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível validar o backup.", exception);
        }
    }

    private FileTime ultimaAlteracao(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException exception) {
            throw new DatabaseException("Não foi possível ler o backup.", exception);
        }
    }
}
