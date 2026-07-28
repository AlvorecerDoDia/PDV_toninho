package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.UsuarioRepository;

/**
 * Autentica usuários ativos e mantém a sessão da aplicação sincronizada.
 */
public final class AutenticacaoService {
    private final UsuarioRepository repository;
    private final PasswordHasher hasher;
    private final SessaoUsuario sessao;

    public AutenticacaoService(
            UsuarioRepository repository, PasswordHasher hasher, SessaoUsuario sessao) {
        this.repository = repository;
        this.hasher = hasher;
        this.sessao = sessao;
    }

    public Usuario autenticar(String login, char[] senha) {
        String normalizedLogin = login == null ? "" : login.strip().toLowerCase();
        Usuario usuario = repository.buscarPorLogin(normalizedLogin)
                .orElseThrow(() -> invalidCredentials());
        if (!usuario.isAtivo()) throw new ValidationException("O usuário está inativo.");
        if (!hasher.verify(senha == null ? new char[0] : senha, usuario.getSenhaHash())) {
            throw invalidCredentials();
        }
        sessao.iniciar(usuario);
        return usuario;
    }

    public void sair() {
        sessao.encerrar();
    }

    private ValidationException invalidCredentials() {
        return new ValidationException("Login ou senha inválidos.");
    }
}
