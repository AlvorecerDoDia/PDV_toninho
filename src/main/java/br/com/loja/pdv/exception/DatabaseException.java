package br.com.loja.pdv.exception;

/** Sinaliza falha técnica de persistência sem expor SQL ao usuário. */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
