package br.com.loja.pdv.config;

import java.nio.file.Path;

/** Resolve todos os arquivos gravaveis fora da pasta de instalacao. */
public final class AppPaths {
    public static final String HOME_PROPERTY = "pdv.home";
    private static final String APP_DIRECTORY = "PDV Toninho";

    private AppPaths() {}

    public static Path baseDirectory() {
        String configured = System.getProperty(HOME_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        Path parent = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"), "AppData", "Local")
                : Path.of(localAppData);
        return parent.resolve(APP_DIRECTORY).toAbsolutePath().normalize();
    }

    public static Path databaseFile() {
        return baseDirectory().resolve("data").resolve("pdv.db");
    }

    public static Path backupDirectory() {
        return baseDirectory().resolve("backups");
    }

    public static Path logDirectory() {
        return baseDirectory().resolve("logs");
    }
}
