package br.com.loja.pdv.infrastructure.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

public final class LoggingConfigurator {
    private LoggingConfigurator() {}

    public static void configurar(Path diretorio) {
        try {
            Files.createDirectories(diretorio);
            Logger root = Logger.getLogger("");
            for (Handler handler : root.getHandlers()) root.removeHandler(handler);

            FileHandler arquivo = new FileHandler(
                    diretorio.resolve("pdv-%g.log").toString(), 2_000_000, 5, true);
            arquivo.setEncoding("UTF-8");
            arquivo.setFormatter(new SimpleFormatter());
            arquivo.setLevel(Level.ALL);
            root.addHandler(arquivo);

            ConsoleHandler console = new ConsoleHandler();
            console.setLevel(Level.WARNING);
            root.addHandler(console);
            root.setLevel(Level.INFO);
        } catch (IOException exception) {
            System.err.println("Não foi possível iniciar o log em arquivo: "
                    + exception.getMessage());
        }
    }
}
