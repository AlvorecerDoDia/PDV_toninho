package br.com.loja.pdv.exception;

/** Indica que a entidade solicitada deixou de existir ou nunca existiu. */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
