package br.com.loja.pdv.domain.model;

import br.com.loja.pdv.domain.enums.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Parcela de pagamento associada a uma forma de recebimento. */
public class Pagamento {
    // Uma venda pode possuir varias parcelas com formas diferentes.
    private Long id;
    private Long vendaId;
    private FormaPagamento forma;
    private BigDecimal valor;
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVendaId() { return vendaId; }
    public void setVendaId(Long vendaId) { this.vendaId = vendaId; }
    public FormaPagamento getForma() { return forma; }
    public void setForma(FormaPagamento forma) { this.forma = forma; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
