package br.com.loja.pdv.domain.model;

import br.com.loja.pdv.domain.enums.TipoMovimentacaoCaixa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Registro imutavel apos persistencia de uma entrada ou saida do caixa. */
public class MovimentacaoCaixa {
    // Cada instancia representa um lancamento financeiro imutavel.
    private Long id;
    private long caixaId;
    private long usuarioId;
    private TipoMovimentacaoCaixa tipo;
    private BigDecimal valor;
    private String motivo;
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getCaixaId() { return caixaId; }
    public void setCaixaId(long caixaId) { this.caixaId = caixaId; }
    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }
    public TipoMovimentacaoCaixa getTipo() { return tipo; }
    public void setTipo(TipoMovimentacaoCaixa tipo) { this.tipo = tipo; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
