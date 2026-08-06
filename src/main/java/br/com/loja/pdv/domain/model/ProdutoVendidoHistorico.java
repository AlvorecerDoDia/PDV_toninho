package br.com.loja.pdv.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Linha historica de um produto vendido em uma venda finalizada. */
public record ProdutoVendidoHistorico(
        long itemVendaId,
        long vendaId,
        String numeroVenda,
        LocalDateTime dataVenda,
        long produtoId,
        String produtoNome,
        Long categoriaId,
        String categoriaNome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal) {
}
