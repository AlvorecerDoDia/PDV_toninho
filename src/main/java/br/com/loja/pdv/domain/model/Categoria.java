package br.com.loja.pdv.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Categoria usada para organizar os produtos do catalogo. */
public class Categoria {
    private Long id;
    private String nome;
    private boolean ativa;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    /** Exibe somente o nome quando a categoria aparece em um ComboBox. */
    @Override
    public String toString() {
        return nome == null ? "" : nome;
    }

    /** Categorias persistidas sao identificadas pelo mesmo ID do banco. */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Categoria categoria)) return false;
        return id != null && Objects.equals(id, categoria.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
