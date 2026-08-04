package br.com.loja.pdv.infrastructure.printing;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Confirma que o texto enviado a impressora usa apenas caracteres seguros. */
class CodificadorComprovanteTest {

    /** Verifica acentos, espaco monetario e sinais tipograficos. */
    @Test
    void deveNormalizarTextoParaImpressoraTermica() {
        String original = "COMPROVANTE NÃO FISCAL\nCafé — R$\u00A07,00\nObrigado pela preferência!";

        String normalizado = CodificadorComprovante.normalizar(original);

        assertEquals(
                "COMPROVANTE NAO FISCAL\nCafe - R$ 7,00\nObrigado pela preferencia!",
                normalizado);
        assertFalse(normalizado.contains("\u00A0"));
    }

    /** Verifica que nenhum byte multibyte UTF-8 chega ao trabalho de impressao. */
    @Test
    void deveCodificarComAscii() {
        String normalizado = "Cafe - R$ 7,00";

        assertArrayEquals(
                normalizado.getBytes(StandardCharsets.US_ASCII),
                CodificadorComprovante.codificar("Café — R$\u00A07,00"));
    }
}
