package br.com.loja.pdv.domain.model;

import br.com.loja.pdv.domain.enums.TipoMovimentacaoEstoque;

import java.time.LocalDateTime;

public class MovimentacaoEstoque {
    private Long id;
    private long produtoId;
    private TipoMovimentacaoEstoque tipo;
    private int quantidade;
    private int quantidadeAnterior;
    private int quantidadePosterior;
    private String motivo;
    private Long usuarioId;
    private Long vendaId;
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getProdutoId() { return produtoId; }
    public void setProdutoId(long produtoId) { this.produtoId = produtoId; }
    public TipoMovimentacaoEstoque getTipo() { return tipo; }
    public void setTipo(TipoMovimentacaoEstoque tipo) { this.tipo = tipo; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public int getQuantidadeAnterior() { return quantidadeAnterior; }
    public void setQuantidadeAnterior(int quantidadeAnterior) { this.quantidadeAnterior = quantidadeAnterior; }
    public int getQuantidadePosterior() { return quantidadePosterior; }
    public void setQuantidadePosterior(int quantidadePosterior) { this.quantidadePosterior = quantidadePosterior; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getVendaId() { return vendaId; }
    public void setVendaId(Long vendaId) { this.vendaId = vendaId; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
