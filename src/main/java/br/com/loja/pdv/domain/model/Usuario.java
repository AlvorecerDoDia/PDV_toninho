package br.com.loja.pdv.domain.model;

import br.com.loja.pdv.domain.enums.PerfilUsuario;

import java.time.LocalDateTime;

/** Usuario autenticavel com perfil, status e hash de senha. */
public class Usuario {
    private Long id;
    private String nome;
    private String login;
    private String senhaHash;
    private PerfilUsuario perfil;
    private boolean ativo;
    private boolean alterarSenha;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public PerfilUsuario getPerfil() { return perfil; }
    public void setPerfil(PerfilUsuario perfil) { this.perfil = perfil; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public boolean isAlterarSenha() { return alterarSenha; }
    public void setAlterarSenha(boolean alterarSenha) { this.alterarSenha = alterarSenha; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
