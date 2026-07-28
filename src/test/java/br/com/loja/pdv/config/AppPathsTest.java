package br.com.loja.pdv.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppPathsTest {
    @TempDir Path tempDirectory;

    @Test
    void deveManterDadosForaDaPastaDoPrograma() {
        String previous = System.getProperty(AppPaths.HOME_PROPERTY);
        try {
            System.setProperty(AppPaths.HOME_PROPERTY, tempDirectory.toString());
            assertEquals(
                    tempDirectory.resolve("data").resolve("pdv.db").toAbsolutePath(),
                    AppPaths.databaseFile());
            assertEquals(
                    tempDirectory.resolve("backups").toAbsolutePath(),
                    AppPaths.backupDirectory());
            assertEquals(
                    tempDirectory.resolve("logs").toAbsolutePath(),
                    AppPaths.logDirectory());
        } finally {
            if (previous == null) System.clearProperty(AppPaths.HOME_PROPERTY);
            else System.setProperty(AppPaths.HOME_PROPERTY, previous);
        }
    }
}
