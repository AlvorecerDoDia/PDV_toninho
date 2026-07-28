package br.com.loja.pdv.exception;

/** Erro de regra de negócio que pode ser mostrado diretamente ao operador. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
