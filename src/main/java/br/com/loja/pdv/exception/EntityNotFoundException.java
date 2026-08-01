package br.com.loja.pdv.exception;

/** Indica que a entidade solicitada deixou de existir ou nunca existiu. */
public class EntityNotFoundException extends RuntimeException {
    /** Cria a excecao com uma mensagem adequada para o tratamento central. */
    public EntityNotFoundException(String message) {
        super(message);
    }
}
