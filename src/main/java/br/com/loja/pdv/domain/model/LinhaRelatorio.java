package br.com.loja.pdv.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LinhaRelatorio(
        String categoria,
        String detalhe,
        Long quantidade,
        BigDecimal valor,
        BigDecimal valorSecundario,
        LocalDateTime data) {
}
