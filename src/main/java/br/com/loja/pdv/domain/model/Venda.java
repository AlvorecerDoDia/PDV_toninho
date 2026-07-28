package br.com.loja.pdv.domain.model;

import br.com.loja.pdv.domain.enums.StatusVenda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Venda {
    private Long id;
    private String numero;
    private long operadorId;
    private long caixaId;
    private StatusVenda status;
    private BigDecimal subtotal;
    private BigDecimal desconto;
    private BigDecimal total;
    private BigDecimal troco;
    private LocalDateTime criadoEm;
    private LocalDateTime canceladoEm;
    private String motivoCancelamento;
    private final List<ItemVenda> itens = new ArrayList<>();
    private final List<Pagamento> pagamentos = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public long getOperadorId() { return operadorId; }
    public void setOperadorId(long operadorId) { this.operadorId = operadorId; }
    public long getCaixaId() { return caixaId; }
    public void setCaixaId(long caixaId) { this.caixaId = caixaId; }
    public StatusVenda getStatus() { return status; }
    public void setStatus(StatusVenda status) { this.status = status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDesconto() { return desconto; }
    public void setDesconto(BigDecimal desconto) { this.desconto = desconto; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BigDecimal getTroco() { return troco; }
    public void setTroco(BigDecimal troco) { this.troco = troco; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(LocalDateTime canceladoEm) { this.canceladoEm = canceladoEm; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }
    public List<ItemVenda> getItens() { return itens; }
    public List<Pagamento> getPagamentos() { return pagamentos; }
}
