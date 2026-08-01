package br.com.loja.pdv.exception;

/** Representa indisponibilidade ou falha da impressora sem invalidar a venda. */
public class ImpressaoException extends RuntimeException {
    /** Cria a excecao com uma mensagem adequada para o tratamento central. */
    public ImpressaoException(String message) {
        super(message);
    }

    /** Cria a excecao com uma mensagem adequada para o tratamento central. */
    public ImpressaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
