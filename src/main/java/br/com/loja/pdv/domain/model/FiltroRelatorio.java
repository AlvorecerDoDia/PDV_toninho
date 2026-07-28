package br.com.loja.pdv.domain.model;

import br.com.loja.pdv.domain.enums.FormaPagamento;

import java.time.LocalDate;

public record FiltroRelatorio(
        LocalDate inicio,
        LocalDate fim,
        Long operadorId,
        FormaPagamento formaPagamento,
        Long produtoId) {
}
