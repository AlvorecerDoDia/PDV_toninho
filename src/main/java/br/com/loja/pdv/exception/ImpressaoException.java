package br.com.loja.pdv.exception;

public class ImpressaoException extends RuntimeException {
    public ImpressaoException(String message) {
        super(message);
    }

    public ImpressaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
