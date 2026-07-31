package br.com.loja.pdv.domain.model;

import java.math.BigDecimal;

/** Item mutavel do carrinho com preco capturado no momento da inclusao. */
public final class ItemCarrinho {
    private final Produto produto;
    private int quantidade;
    private final BigDecimal precoUnitario;

    ItemCarrinho(Produto produto, int quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPrecoVenda();
    }

    public Produto getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
