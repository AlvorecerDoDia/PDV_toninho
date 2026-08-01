package br.com.loja.pdv;

import javafx.application.Application;

/** Inicializador separado necessario para o JavaFX funcionar no pacote jpackage. */
public final class PdvLauncher {
    /** Impede a criacao de instancias de uma classe formada apenas por funcoes utilitarias. */
    private PdvLauncher() {}

    /**
     * Inicia o JavaFX por uma classe sem heranca, requisito importante para o executavel
     * empacotado.
     */
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
