package br.com.loja.pdv.exception;

/** Informa que um codigo de barras ja pertence a outro produto. */
public class DuplicateBarcodeException extends RuntimeException {
    /** Cria a excecao com uma mensagem adequada para o tratamento central. */
    public DuplicateBarcodeException(String message) {
        super(message);
    }
}
