package br.com.loja.pdv.util;

import java.math.BigDecimal;

/** Converte valores monetarios entre BigDecimal e centavos inteiros sem arredondar. */
public final class MoneyUtils {

    /** Impede a criacao de instancias de uma classe formada apenas por funcoes utilitarias. */
    private MoneyUtils() {
    }

    /** Converte BigDecimal em centavos inteiros sem aceitar arredondamento implicito. */
    public static long toCents(BigDecimal value) {
        return value
                .movePointRight(2)
                .longValueExact();
    }

    /** Converte centavos persistidos em BigDecimal com duas casas. */
    public static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
