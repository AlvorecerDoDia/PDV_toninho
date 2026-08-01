package br.com.loja.pdv.exception;

/** Sinaliza falha tecnica de persistencia sem expor SQL ao usuario. */
public class DatabaseException extends RuntimeException {

    /** Cria a excecao com uma mensagem adequada para o tratamento central. */
    public DatabaseException(String message) {
        super(message);
    }

    /** Cria a excecao com uma mensagem adequada para o tratamento central. */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
