package br.com.loja.pdv.infrastructure.backup;

import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** Testa um componente de infraestrutura com recursos controlados pelo teste. */
class GerenciadorBackupTest {
    @TempDir Path tempDirectory;
    private Database database;
    private GerenciadorBackup manager;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database(tempDirectory.resolve("data").resolve("pdv.db"));
        new DatabaseInitializer(database).initialize();
        manager = new GerenciadorBackup(database, tempDirectory.resolve("backups"));
        insertProduct("Original");
    }

    /** Verifica o cenario: deve criar backup consistente. */
    @Test
    void deveCriarBackupConsistente() throws Exception {
        Path backup = manager.criar("manual");
        assertTrue(Files.isRegularFile(backup));
        assertTrue(Files.size(backup) > 0);
        assertEquals(1, countProducts(new Database(backup)));
    }

    /** Verifica o cenario: deve restaurar ecriar copia de seguranca anterior. */
    @Test
    void deveRestaurarECriarCopiaDeSegurancaAnterior() throws Exception {
        Path backup = manager.criar("base");
        insertProduct("Posterior");
        assertEquals(2, countProducts(database));
        Path safety = manager.restaurar(backup);
        assertTrue(Files.isRegularFile(safety));
        assertEquals(1, countProducts(database));
        assertEquals(2, countProducts(new Database(safety)));
    }

    /** Verifica o cenario: deve recusar arquivo invalido. */
    @Test
    void deveRecusarArquivoInvalido() throws Exception {
        Path invalid = tempDirectory.resolve("invalido.db");
        Files.writeString(invalid, "não é sqlite");
        assertThrows(DatabaseException.class, () -> manager.restaurar(invalid));
    }

    /** Verifica o cenario: deve aplicar retencao dos backups mais recentes. */
    @Test
    void deveAplicarRetencaoDosBackupsMaisRecentes() throws Exception {
        Path first = manager.criar("primeiro");
        Path second = manager.criar("segundo");
        Path third = manager.criar("terceiro");
        Files.setLastModifiedTime(first, FileTime.from(Instant.parse("2026-01-01T00:00:00Z")));
        Files.setLastModifiedTime(second, FileTime.from(Instant.parse("2026-01-02T00:00:00Z")));
        Files.setLastModifiedTime(third, FileTime.from(Instant.parse("2026-01-03T00:00:00Z")));
        manager.aplicarRetencao(2);
        assertEquals(2, manager.listar().size());
        assertFalse(Files.exists(first));
    }

    /** Verifica o cenario: deve informar erro quando diretorio nao pode ser criado. */
    @Test
    void deveInformarErroQuandoDiretorioNaoPodeSerCriado() throws Exception {
        Path occupied = tempDirectory.resolve("arquivo-no-lugar-da-pasta");
        Files.writeString(occupied, "ocupado");
        GerenciadorBackup invalidManager = new GerenciadorBackup(database, occupied);
        assertThrows(DatabaseException.class, () -> invalidManager.criar("manual"));
    }

    private void insertProduct(String name) throws Exception {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO produto (
                        nome, preco_custo_centavos, preco_venda_centavos,
                        quantidade_estoque, estoque_minimo, ativo, criado_em, atualizado_em
                    ) VALUES ('%s', 100, 200, 0, 0, 1,
                              '2026-01-01T00:00:00', '2026-01-01T00:00:00')
                    """.formatted(name));
        }
    }

    private int countProducts(Database value) throws Exception {
        try (Connection connection = value.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM produto")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
