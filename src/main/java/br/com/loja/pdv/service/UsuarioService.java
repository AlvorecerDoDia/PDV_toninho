package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.PerfilUsuario;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.EntityNotFoundException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.UsuarioRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public final class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository repository, PasswordHasher hasher) {
        this(repository, hasher, null, Clock.systemDefaultZone());
    }

    public UsuarioService(
            UsuarioRepository repository, PasswordHasher hasher,
            AuditoriaService auditoria) {
        this(repository, hasher, auditoria, Clock.systemDefaultZone());
    }

    UsuarioService(UsuarioRepository repository, PasswordHasher hasher, Clock clock) {
        this(repository, hasher, null, clock);
    }

    UsuarioService(
            UsuarioRepository repository, PasswordHasher hasher,
            AuditoriaService auditoria, Clock clock) {
        this.repository = repository;
        this.hasher = hasher;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    public Usuario criar(
            String nome, String login, char[] senha, PerfilUsuario perfil, boolean alterarSenha) {
        validatePassword(senha);
        LocalDateTime now = LocalDateTime.now(clock);
        Usuario usuario = new Usuario();
        usuario.setNome(normalizeRequired(nome, "nome"));
        usuario.setLogin(normalizeRequired(login, "login").toLowerCase());
        usuario.setSenhaHash(hasher.hash(senha));
        usuario.setPerfil(perfil == null ? PerfilUsuario.OPERADOR : perfil);
        usuario.setAtivo(true);
        usuario.setAlterarSenha(alterarSenha);
        usuario.setCriadoEm(now);
        usuario.setAtualizadoEm(now);
        Usuario salvo = repository.salvar(usuario);
        audit("CRIACAO_USUARIO", salvo.getId(), null, safeValues(salvo));
        return salvo;
    }

    public Usuario criarAdministradorInicial(char[] senha) {
        if (repository.contar() != 0) {
            throw new ValidationException("O administrador inicial já foi criado.");
        }
        return criar("Administrador", "admin", senha, PerfilUsuario.ADMINISTRADOR, true);
    }

    public void atualizar(long id, String nome, String login, PerfilUsuario perfil, boolean ativo) {
        Usuario usuario = buscar(id);
        String anterior = safeValues(usuario);
        usuario.setNome(normalizeRequired(nome, "nome"));
        usuario.setLogin(normalizeRequired(login, "login").toLowerCase());
        usuario.setPerfil(perfil == null ? usuario.getPerfil() : perfil);
        usuario.setAtivo(ativo);
        usuario.setAtualizadoEm(LocalDateTime.now(clock));
        repository.atualizar(usuario);
        audit("ALTERACAO_USUARIO", id, anterior, safeValues(usuario));
    }

    public void trocarSenha(long id, char[] senha) {
        validatePassword(senha);
        Usuario usuario = buscar(id);
        usuario.setSenhaHash(hasher.hash(senha));
        usuario.setAlterarSenha(false);
        usuario.setAtualizadoEm(LocalDateTime.now(clock));
        repository.atualizar(usuario);
        audit("TROCA_SENHA", id, null, "senha=alterada");
    }

    public Usuario buscar(long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }

    public List<Usuario> listar() {
        return repository.listar();
    }

    private void validatePassword(char[] senha) {
        if (senha == null || senha.length < 8) {
            throw new ValidationException("A senha deve ter pelo menos 8 caracteres.");
        }
    }

    private String normalizeRequired(String value, String field) {
        String normalized = value == null ? "" : value.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) throw new ValidationException("O " + field + " é obrigatório.");
        return normalized;
    }

    private String safeValues(Usuario usuario) {
        return "nome=" + usuario.getNome() + "; login=" + usuario.getLogin()
                + "; perfil=" + usuario.getPerfil() + "; ativo=" + usuario.isAtivo();
    }

    private void audit(String action, Long id, String before, String after) {
        if (auditoria != null) {
            auditoria.registrar(action, "USUARIO", id, before, after);
        }
    }
}
