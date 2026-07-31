package br.com.loja.pdv.exception;

/** Sinaliza falha tecnica de persistencia sem expor SQL ao usuario. */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
