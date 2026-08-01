package br.com.loja.pdv.controller;

import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.ImpressaoException;
import br.com.loja.pdv.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/** Testa o comportamento compartilhado pela camada de interface. */
class ErrorHandlerTest {
    /** Verifica o cenario: deve preservar mensagem de validacao. */
    @Test
    void devePreservarMensagemDeValidacao() {
        assertEquals("Campo obrigatório.",
                ErrorHandler.mensagem(new ValidationException("Campo obrigatório.")));
    }

    /** Verifica o cenario: deve ocultar detalhe tecnico do banco. */
    @Test
    void deveOcultarDetalheTecnicoDoBanco() {
        String mensagem = ErrorHandler.mensagem(new DatabaseException(
                "SQL secreto", new SQLException("arquivo e tabela")));
        assertFalse(mensagem.contains("SQL"));
        assertFalse(mensagem.contains("tabela"));
    }

    /** Verifica o cenario: deve informar falha de impressao sem perder contexto. */
    @Test
    void deveInformarFalhaDeImpressaoSemPerderContexto() {
        assertEquals("Impressora indisponível.",
                ErrorHandler.mensagem(new ImpressaoException("Impressora indisponível.")));
    }

    /** Verifica o cenario: deve tratar erro inesperado sem expor excecao. */
    @Test
    void deveTratarErroInesperadoSemExporExcecao() {
        String mensagem = ErrorHandler.mensagem(new IllegalStateException("segredo"));
        assertTrue(mensagem.contains("inesperado"));
        assertFalse(mensagem.contains("segredo"));
    }
}
