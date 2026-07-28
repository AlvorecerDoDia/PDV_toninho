package br.com.loja.pdv.domain.model;

import java.time.LocalDateTime;

public class RegistroAuditoria {
    private Long id;
    private Long usuarioId;
    private String acao;
    private String entidade;
    private Long entidadeId;
    private String valoresAnteriores;
    private String valoresNovos;
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }
    public Long getEntidadeId() { return entidadeId; }
    public void setEntidadeId(Long entidadeId) { this.entidadeId = entidadeId; }
    public String getValoresAnteriores() { return valoresAnteriores; }
    public void setValoresAnteriores(String valoresAnteriores) { this.valoresAnteriores = valoresAnteriores; }
    public String getValoresNovos() { return valoresNovos; }
    public void setValoresNovos(String valoresNovos) { this.valoresNovos = valoresNovos; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
