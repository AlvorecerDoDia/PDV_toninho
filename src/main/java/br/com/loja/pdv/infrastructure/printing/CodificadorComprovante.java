package br.com.loja.pdv.infrastructure.printing;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Converte o texto Unicode do comprovante para caracteres ASCII seguros.
 * Impressoras termicas simples costumam interpretar bytes UTF-8 como outra
 * pagina de codigo, o que corrompe acentos e o espaco usado pelo formato BRL.
 */
final class CodificadorComprovante {
    private CodificadorComprovante() {}

    /** Normaliza o texto e devolve os bytes enviados ao driver da impressora. */
    static byte[] codificar(String texto) {
        return normalizar(texto).getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Remove acentos, troca espacos especiais e substitui sinais tipograficos
     * por equivalentes que existem na tabela ASCII basica.
     */
    static String normalizar(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("O texto do comprovante é obrigatório.");
        }

        String compativel = texto
                .replace('\u00A0', ' ')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201C', '"')
                .replace('\u201D', '"')
                .replace("º", "o")
                .replace("ª", "a");

        String semAcentos = Normalizer.normalize(compativel, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        StringBuilder resultado = new StringBuilder(semAcentos.length());
        for (int indice = 0; indice < semAcentos.length(); indice++) {
            char caractere = semAcentos.charAt(indice);
            if (caractere == '\n' || caractere == '\r' || caractere == '\t'
                    || caractere >= 32 && caractere <= 126) {
                resultado.append(caractere);
            } else {
                resultado.append('?');
            }
        }
        return resultado.toString();
    }
}
