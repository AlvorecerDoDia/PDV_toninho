package br.com.loja.pdv.infrastructure.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

/** Configura um unico arquivo de log e a saida de avisos no console. */
public final class LoggingConfigurator {
    private LoggingConfigurator() {}

    public static void configurar(Path diretorio) {
        try {
            Files.createDirectories(diretorio);
            Logger raiz = Logger.getLogger("");
            for (Handler handler : raiz.getHandlers()) raiz.removeHandler(handler);

            FileHandler arquivo = new FileHandler(
                    diretorio.resolve("pdv.log").toString(), true);
            arquivo.setEncoding("UTF-8");
            arquivo.setFormatter(new SimpleFormatter());
            arquivo.setLevel(Level.ALL);
            raiz.addHandler(arquivo);

            ConsoleHandler console = new ConsoleHandler();
            console.setLevel(Level.WARNING);
            raiz.addHandler(console);
            raiz.setLevel(Level.INFO);
        } catch (IOException exception) {
            System.err.println("Não foi possível iniciar o log em arquivo: "
                    + exception.getMessage());
        }
    }
}
