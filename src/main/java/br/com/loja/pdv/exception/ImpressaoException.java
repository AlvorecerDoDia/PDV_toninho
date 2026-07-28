package br.com.loja.pdv.exception;

/** Representa indisponibilidade ou falha da impressora sem invalidar a venda. */
public class ImpressaoException extends RuntimeException {
    public ImpressaoException(String message) {
        super(message);
    }

    public ImpressaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
