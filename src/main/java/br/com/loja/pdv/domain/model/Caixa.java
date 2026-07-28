package br.com.loja.pdv.domain.model;

import br.com.loja.pdv.domain.enums.StatusCaixa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representa a abertura e o eventual fechamento do caixa de um operador. */
public class Caixa {
    private Long id;
    private long usuarioId;
    private StatusCaixa status;
    private BigDecimal valorAbertura;
    private BigDecimal valorEsperado;
    private BigDecimal valorContado;
    private BigDecimal diferenca;
    private LocalDateTime abertoEm;
    private LocalDateTime fechadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }
    public StatusCaixa getStatus() { return status; }
    public void setStatus(StatusCaixa status) { this.status = status; }
    public BigDecimal getValorAbertura() { return valorAbertura; }
    public void setValorAbertura(BigDecimal valorAbertura) { this.valorAbertura = valorAbertura; }
    public BigDecimal getValorEsperado() { return valorEsperado; }
    public void setValorEsperado(BigDecimal valorEsperado) { this.valorEsperado = valorEsperado; }
    public BigDecimal getValorContado() { return valorContado; }
    public void setValorContado(BigDecimal valorContado) { this.valorContado = valorContado; }
    public BigDecimal getDiferenca() { return diferenca; }
    public void setDiferenca(BigDecimal diferenca) { this.diferenca = diferenca; }
    public LocalDateTime getAbertoEm() { return abertoEm; }
    public void setAbertoEm(LocalDateTime abertoEm) { this.abertoEm = abertoEm; }
    public LocalDateTime getFechadoEm() { return fechadoEm; }
    public void setFechadoEm(LocalDateTime fechadoEm) { this.fechadoEm = fechadoEm; }
}
