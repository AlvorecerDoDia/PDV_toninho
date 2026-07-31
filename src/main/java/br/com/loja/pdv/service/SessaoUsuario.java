package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.Permissao;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.ValidationException;

import java.util.Optional;

/**
 * Mantem o usuario autenticado em memoria e verifica suas permissoes.
 */
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

    public void encerrar() {
        usuario = null;
    }

    public void exigir(Permissao permissao) {
        Usuario atual = atual().orElseThrow(() -> new ValidationException("Faça login para continuar."));
        if (!atual.getPerfil().permite(permissao)) {
            throw new ValidationException("Seu perfil não possui permissão para esta função.");
        }
    }
}
