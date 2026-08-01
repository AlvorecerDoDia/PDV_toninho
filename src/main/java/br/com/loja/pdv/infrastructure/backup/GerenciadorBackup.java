package br.com.loja.pdv.infrastructure.backup;

import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** Executa backup SQLite consistente, validacao, retencao e restauracao. */
public final class GerenciadorBackup {
    private static final DateTimeFormatter NAME_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private final Database database;
    private final Path backupDirectory;
    private final Clock clock;

    /** Recebe o banco de origem e a pasta onde as copias serao mantidas. */
    public GerenciadorBackup(Database database, Path backupDirectory) {
        this(database, backupDirectory, Clock.systemDefaultZone());
    }

    /** Variante usada pelos testes para controlar o relogio. */
    GerenciadorBackup(Database database, Path backupDirectory, Clock clock) {
        this.database = database;
        this.backupDirectory = backupDirectory.toAbsolutePath().normalize();
        this.clock = clock;
    }

    /** Cria uma copia consistente do SQLite e devolve o caminho gerado. */
    public Path criar(String prefix) {
        try {
            Files.createDirectories(backupDirectory);
            String safePrefix = prefix == null || prefix.isBlank()
                    ? "backup" : prefix.replaceAll("[^a-zA-Z0-9_-]", "-");
            Path target = backupDirectory.resolve(
                    safePrefix + "-" + LocalDateTime.now(clock).format(NAME_DATE) + ".db");
            // O SQLite produz uma copia consistente mesmo com o banco em uso.
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("VACUUM INTO ?")) {
                statement.setString(1, target.toString());
                statement.execute();
            }
            if (!Files.isRegularFile(target) || Files.size(target) == 0) {
                throw new DatabaseException("O arquivo de backup não foi criado corretamente.");
            }
            validar(target);
            return target;
        } catch (SQLException | IOException exception) {
            throw new DatabaseException("Não foi possível criar o backup.", exception);
        }
    }

    /** Valida o arquivo, cria uma copia de seguranca e substitui o banco de forma atomica. */
    public Path restaurar(Path source) {
        Path normalized = source == null ? null : source.toAbsolutePath().normalize();
        if (normalized == null || !Files.isRegularFile(normalized)) {
            throw new DatabaseException("Selecione um backup válido.");
        }
        validar(normalized);
        // Preserva o estado atual para permitir recuperacao caso o arquivo escolhido
        // esteja integro, mas nao contenha os dados esperados pelo operador.
        Path safety = criar("antes-restauracao");
        Path target = database.getDatabaseFile();
        Path temporary = target.resolveSibling(target.getFileName() + ".restore.tmp");
        try {
            Files.copy(normalized, temporary, StandardCopyOption.REPLACE_EXISTING);
            // A troca atomica impede que uma interrupcao deixe um banco parcialmente copiado.
            Files.move(temporary, target,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            deleteSidecars(target);
            new DatabaseInitializer(database).initialize();
            return safety;
        } catch (AtomicMoveNotSupportedException exception) {
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                deleteSidecars(target);
                new DatabaseInitializer(database).initialize();
                return safety;
            } catch (IOException fallback) {
                fallback.addSuppressed(exception);
                throw new DatabaseException("Não foi possível restaurar o backup.", fallback);
            }
        } catch (IOException exception) {
            throw new DatabaseException("Não foi possível restaurar o backup.", exception);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException exception) {
                temporary.toFile().deleteOnExit();
            }
        }
    }

    /** Mantem somente a quantidade mais recente de backups configurada. */
    public void aplicarRetencao(int quantidade) {
        if (quantidade < 1) throw new IllegalArgumentException("Retenção inválida.");
        try {
            if (!Files.isDirectory(backupDirectory)) return;
            List<Path> files;
            try (var stream = Files.list(backupDirectory)) {
                files = stream.filter(path -> path.getFileName().toString().endsWith(".db"))
                        .sorted(Comparator.comparing(this::lastModified).reversed())
                        .toList();
            }
            for (int index = quantidade; index < files.size(); index++) {
                Files.deleteIfExists(files.get(index));
            }
        } catch (IOException exception) {
            throw new DatabaseException("Não foi possível aplicar a retenção de backups.", exception);
        }
    }

    /** Lista arquivos de banco ordenados do mais recente para o mais antigo. */
    public List<Path> listar() {
        try {
            if (!Files.isDirectory(backupDirectory)) return List.of();
            try (var stream = Files.list(backupDirectory)) {
                return stream.filter(path -> path.getFileName().toString().endsWith(".db"))
                        .sorted(Comparator.comparing(this::lastModified).reversed())
                        .toList();
            }
        } catch (IOException exception) {
            throw new DatabaseException("Não foi possível listar os backups.", exception);
        }
    }

    /** Abre o arquivo como SQLite e executa a verificacao de integridade. */
    private void validar(Path file) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA quick_check")) {
            if (!resultSet.next() || !"ok".equalsIgnoreCase(resultSet.getString(1))) {
                throw new DatabaseException("O arquivo de backup está corrompido.");
            }
            try (ResultSet schema = statement.executeQuery("""
                    SELECT COUNT(*) FROM sqlite_master
                    WHERE type = 'table' AND name = 'schema_version'
                    """)) {
                if (!schema.next() || schema.getInt(1) != 1) {
                    throw new DatabaseException("O arquivo não pertence ao PDV Toninho.");
                }
            }
        } catch (SQLException exception) {
            throw new DatabaseException("O arquivo não é um banco SQLite válido.", exception);
        }
    }

    /** Le a data do arquivo e transforma falhas de IO em erro de dominio. */
    private FileTime lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException exception) {
            throw new DatabaseException("Não foi possível ler o backup.", exception);
        }
    }

    /** Remove arquivos WAL e SHM que pertenciam ao banco anterior. */
    private void deleteSidecars(Path target) throws IOException {
        // WAL e SHM pertencem ao banco anterior e nao podem acompanhar a restauracao.
        Files.deleteIfExists(Path.of(target + "-wal"));
        Files.deleteIfExists(Path.of(target + "-shm"));
    }
}
