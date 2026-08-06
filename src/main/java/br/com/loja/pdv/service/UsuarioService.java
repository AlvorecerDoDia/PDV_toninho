package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.EntityNotFoundException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.UsuarioRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Gerencia contas locais sem perfis ou permissoes diferentes. */
public final class UsuarioService {
    private static final char[] SENHA_INICIAL = "admin".toCharArray();
    private final UsuarioRepository repository;
    private final PasswordHasher hasher;
    private final Clock clock;

    public UsuarioService(UsuarioRepository repository, PasswordHasher hasher) {
        this(repository, hasher, Clock.systemDefaultZone());
    }

    UsuarioService(UsuarioRepository repository, PasswordHasher hasher, Clock clock) {
        this.repository = repository;
        this.hasher = hasher;
        this.clock = clock;
    }

    /** Cria a conta admin apenas quando o banco ainda nao possui usuarios. */
    public boolean configurarUsuarioInicialPadrao() {
        if (repository.contar() != 0) return false;
        criarInterno("Administrador", "admin", SENHA_INICIAL, true);
        return true;
    }

    public Usuario criar(String nome, String login, char[] senha) {
        validarSenha(senha);
        return criarInterno(
                normalizarObrigatorio(nome, "nome"),
                normalizarObrigatorio(login, "login").toLowerCase(),
                senha,
                true);
    }

    public void atualizar(long id, String nome, String login, boolean ativo) {
        Usuario usuario = buscar(id);
        usuario.setNome(normalizarObrigatorio(nome, "nome"));
        usuario.setLogin(normalizarObrigatorio(login, "login").toLowerCase());
        usuario.setAtivo(ativo);
        usuario.setAtualizadoEm(LocalDateTime.now(clock));
        repository.atualizar(usuario);
    }

    public void trocarSenha(long id, char[] senha) {
        validarSenha(senha);
        Usuario usuario = buscar(id);
        usuario.setSenhaHash(hasher.hash(senha));
        usuario.setAtualizadoEm(LocalDateTime.now(clock));
        repository.atualizar(usuario);
    }

    public Usuario buscar(long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }

    public List<Usuario> listar() {
        return repository.listar();
    }

    private Usuario criarInterno(
            String nome, String login, char[] senha, boolean ativo) {
        LocalDateTime agora = LocalDateTime.now(clock);
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setLogin(login);
        usuario.setSenhaHash(hasher.hash(senha));
        usuario.setAtivo(ativo);
        usuario.setCriadoEm(agora);
        usuario.setAtualizadoEm(agora);
        return repository.salvar(usuario);
    }

    private void validarSenha(char[] senha) {
        if (senha == null || senha.length < 4) {
            throw new ValidationException("A senha deve ter pelo menos 4 caracteres.");
        }
    }

    private String normalizarObrigatorio(String valor, String campo) {
        String normalizado = valor == null ? "" : valor.strip().replaceAll("\\s+", " ");
        if (normalizado.isEmpty()) {
            throw new ValidationException("O " + campo + " é obrigatório.");
        }
        return normalizado;
    }
}
