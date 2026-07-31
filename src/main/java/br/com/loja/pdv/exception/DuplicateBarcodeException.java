package br.com.loja.pdv.exception;

/** Informa que um codigo de barras ja pertence a outro produto. */
public class DuplicateBarcodeException extends RuntimeException {
    public DuplicateBarcodeException(String message) {
        super(message);
    }
}
