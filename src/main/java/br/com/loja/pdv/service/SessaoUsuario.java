package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.ValidationException;

import java.util.Optional;

/** Mantem somente o usuario autenticado durante a execucao. */
public final class SessaoUsuario {
    private Usuario usuario;

    public void iniciar(Usuario usuario) {
        if (usuario == null || !usuario.isAtivo()) {
            throw new ValidationException("Não é possível iniciar a sessão.");
        }
        this.usuario = usuario;
    }

    public Optional<Usuario> atual() {
        return Optional.ofNullable(usuario);
    }

    public Usuario exigirLogin() {
        return atual().orElseThrow(() ->
                new ValidationException("Faça login para continuar."));
    }

    public void encerrar() {
        usuario = null;
    }
}
