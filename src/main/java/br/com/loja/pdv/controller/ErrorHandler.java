package br.com.loja.pdv.controller;

import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.ImpressaoException;
import br.com.loja.pdv.exception.ValidationException;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class ErrorHandler {
    private static final Logger LOGGER = Logger.getLogger(ErrorHandler.class.getName());

    private ErrorHandler() {}

    public static String mensagem(Throwable error) {
        if (error instanceof ValidationException) {
            LOGGER.log(Level.FINE, "Validação recusada: {0}", error.getMessage());
            return safe(error.getMessage(), "Verifique os dados informados.");
        }
        if (error instanceof DatabaseException) {
            LOGGER.log(Level.SEVERE, "Falha de banco de dados", error);
            return "Não foi possível acessar os dados. Tente novamente.";
        }
        if (error instanceof ImpressaoException) {
            LOGGER.log(Level.WARNING, "Falha de impressão", error);
            return safe(error.getMessage(), "Não foi possível imprimir o comprovante.");
        }
        if (error instanceof NumberFormatException) {
            LOGGER.log(Level.FINE, "Número inválido informado", error);
            return "Informe valores numéricos válidos.";
        }
        LOGGER.log(Level.SEVERE, "Erro inesperado", error);
        return "Ocorreu um erro inesperado. Consulte o log ou tente novamente.";
    }

    public static void registrarInesperado(
            Thread thread, Throwable error) {
        LOGGER.log(Level.SEVERE,
                "Erro não tratado na thread " + thread.getName(), error);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
