package br.com.loja.pdv.exception;

/** Erro de regra de negocio que pode ser mostrado diretamente ao operador. */
public class ValidationException extends RuntimeException {
    /** Cria a excecao com uma mensagem adequada para o tratamento central. */
    public ValidationException(String message) {
        super(message);
    }
}
