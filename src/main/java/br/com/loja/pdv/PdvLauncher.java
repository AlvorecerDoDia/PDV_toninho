package br.com.loja.pdv;

import javafx.application.Application;

/** Inicializador separado necessário para o JavaFX funcionar no pacote jpackage. */
public final class PdvLauncher {
    private PdvLauncher() {}

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
