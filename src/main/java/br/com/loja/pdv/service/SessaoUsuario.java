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

    /** Guarda o usuario autenticado na memoria da aplicacao. */
    public void iniciar(Usuario usuario) {
        if (usuario == null || !usuario.isAtivo()) {
            throw new ValidationException("Não é possível iniciar a sessão.");
        }
        this.usuario = usuario;
    }

    /** Retorna o usuario atual como Optional para explicitar a possivel ausencia. */
    public Optional<Usuario> atual() {
        return Optional.ofNullable(usuario);
    }

    /** Remove qualquer usuario da sessao. */
    public void encerrar() {
        usuario = null;
    }

    /** Bloqueia o caso de uso quando nao existe sessao ou permissao suficiente. */
    public void exigir(Permissao permissao) {
        Usuario atual = atual().orElseThrow(() -> new ValidationException("Faça login para continuar."));
        if (!atual.getPerfil().permite(permissao)) {
            throw new ValidationException("Seu perfil não possui permissão para esta função.");
        }
    }
}
