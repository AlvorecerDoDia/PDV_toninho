package br.com.loja.pdv.controller;

import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.ImpressaoException;
import br.com.loja.pdv.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlerTest {
    @Test
    void devePreservarMensagemDeValidacao() {
        assertEquals("Campo obrigatório.",
                ErrorHandler.mensagem(new ValidationException("Campo obrigatório.")));
    }

    @Test
    void deveOcultarDetalheTecnicoDoBanco() {
        String mensagem = ErrorHandler.mensagem(new DatabaseException(
                "SQL secreto", new SQLException("arquivo e tabela")));
        assertFalse(mensagem.contains("SQL"));
        assertFalse(mensagem.contains("tabela"));
    }

    @Test
    void deveInformarFalhaDeImpressaoSemPerderContexto() {
        assertEquals("Impressora indisponível.",
                ErrorHandler.mensagem(new ImpressaoException("Impressora indisponível.")));
    }

    @Test
    void deveTratarErroInesperadoSemExporExcecao() {
        String mensagem = ErrorHandler.mensagem(new IllegalStateException("segredo"));
        assertTrue(mensagem.contains("inesperado"));
        assertFalse(mensagem.contains("segredo"));
    }
}
