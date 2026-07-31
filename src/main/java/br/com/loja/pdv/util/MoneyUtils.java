package br.com.loja.pdv.util;

import java.math.BigDecimal;

/** Converte valores monetarios entre BigDecimal e centavos inteiros sem arredondar. */
public final class MoneyUtils {

    private MoneyUtils() {
    }

    public static long toCents(BigDecimal value) {
        return value
                .movePointRight(2)
                .longValueExact();
    }

    public static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
