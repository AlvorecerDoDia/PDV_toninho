package br.com.loja.pdv.config;

import java.nio.file.Path;

/** Resolve todos os arquivos gravaveis fora da pasta de instalacao. */
public final class AppPaths {
    public static final String HOME_PROPERTY = "pdv.home";
    private static final String APP_DIRECTORY = "PDV Toninho";

    /** Impede a criacao de instancias de uma classe formada apenas por funcoes utilitarias. */
    private AppPaths() {}

    /** Resolve a pasta raiz de dados, permitindo substituicao por propriedade da JVM em testes. */
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

    /** Retorna o caminho completo do arquivo SQLite. */
    public static Path databaseFile() {
        return baseDirectory().resolve("data").resolve("pdv.db");
    }

    /** Retorna a pasta usada para armazenar copias de seguranca. */
    public static Path backupDirectory() {
        return baseDirectory().resolve("backups");
    }

    /** Retorna a pasta usada pelos arquivos de log rotativos. */
    public static Path logDirectory() {
        return baseDirectory().resolve("logs");
    }
}
