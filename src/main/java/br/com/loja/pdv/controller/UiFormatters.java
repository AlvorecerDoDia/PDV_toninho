package br.com.loja.pdv.controller;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/** Instala filtros de entrada reutilizaveis nos campos JavaFX. */
public final class UiFormatters {
    /** Recebe os servicos e objetos de sessao usados pelas acoes desta tela. */
    private UiFormatters() {}

    /** Restringe campos monetarios a digitos e no maximo duas casas decimais. */
    public static void moeda(TextField... campos) {
        for (TextField campo : campos) {
            campo.setPromptText("0,00");
            campo.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().matches("\\d{0,9}([.,]\\d{0,2})?")
                            ? change : null));
        }
    }

    /** Restringe campos de quantidade a numeros inteiros nao negativos. */
    public static void inteiro(TextField... campos) {
        for (TextField campo : campos) {
            campo.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().matches("\\d{0,9}") ? change : null));
        }
    }
}
