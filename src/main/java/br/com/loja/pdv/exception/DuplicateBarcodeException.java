package br.com.loja.pdv.exception;

/** Informa que um código de barras já pertence a outro produto. */
public class DuplicateBarcodeException extends RuntimeException {
    public DuplicateBarcodeException(String message) {
        super(message);
    }
}
