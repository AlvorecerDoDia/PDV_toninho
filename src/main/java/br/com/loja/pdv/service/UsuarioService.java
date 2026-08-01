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

/**
 * Gerencia usuarios, credenciais e a configuracao segura do acesso administrativo inicial.
 */
public final class UsuarioService {
    private static final char[] SENHA_ADMIN_INICIAL = "admin".toCharArray();
    private final UsuarioRepository repository;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final AuditoriaService auditoria;

    /** Recebe as dependencias necessarias para aplicar as regras deste caso de uso. */
    public UsuarioService(UsuarioRepository repository, PasswordHasher hasher) {
        this(repository, hasher, null, Clock.systemDefaultZone());
    }

    /** Recebe as dependencias necessarias para aplicar as regras deste caso de uso. */
    public UsuarioService(
            UsuarioRepository repository, PasswordHasher hasher,
            AuditoriaService auditoria) {
        this(repository, hasher, auditoria, Clock.systemDefaultZone());
    }

    /** Variante usada pelos testes sem auditoria e com relogio controlado. */
    UsuarioService(UsuarioRepository repository, PasswordHasher hasher, Clock clock) {
        this(repository, hasher, null, clock);
    }

    /** Construtor completo que recebe persistencia, seguranca, auditoria e relogio. */
    UsuarioService(
            UsuarioRepository repository, PasswordHasher hasher,
            AuditoriaService auditoria, Clock clock) {
        this.repository = repository;
        this.hasher = hasher;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    /** Valida permissao, senha e dados antes de criar um usuario. */
    public Usuario criar(
            String nome, String login, char[] senha, PerfilUsuario perfil, boolean alterarSenha) {
        validatePassword(senha);
        return criarSemValidarSenha(
                normalizeRequired(nome, "nome"),
                normalizeRequired(login, "login").toLowerCase(),
                senha,
                perfil == null ? PerfilUsuario.OPERADOR : perfil,
                alterarSenha);
    }

    /** Cria a primeira conta administrativa quando o banco esta vazio. */
    public Usuario criarAdministradorInicial(char[] senha) {
        if (repository.contar() != 0) {
            throw new ValidationException("O administrador inicial já foi criado.");
        }
        return criar("Administrador", "admin", senha, PerfilUsuario.ADMINISTRADOR, true);
    }

    /** Garante a credencial temporaria somente enquanto a troca ainda e obrigatoria. */
    public boolean configurarAdministradorInicialPadrao() {
        if (repository.contar() == 0) {
            // "admin" e temporaria e obriga a troca logo apos o primeiro acesso.
            criarSemValidarSenha(
                    "Administrador", "admin", SENHA_ADMIN_INICIAL,
                    PerfilUsuario.ADMINISTRADOR, true);
            return true;
        }
        // Atualiza instalacoes antigas somente enquanto a troca inicial ainda esta
        // pendente; uma senha que o administrador ja escolheu nunca e sobrescrita.
        return repository.buscarPorLogin("admin")
                .filter(usuario -> usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR)
                .filter(Usuario::isAlterarSenha)
                .map(usuario -> {
                    usuario.setSenhaHash(hasher.hash(SENHA_ADMIN_INICIAL));
                    usuario.setAtualizadoEm(LocalDateTime.now(clock));
                    repository.atualizar(usuario);
                    audit("CREDENCIAL_INICIAL_ADMIN", usuario.getId(), null,
                            "senha_temporaria=redefinida");
                    return true;
                })
                .orElse(false);
    }

    /** Altera dados e perfil sem exigir uma nova senha. */
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

    /** Valida e substitui o hash, removendo a obrigacao de troca. */
    public void trocarSenha(long id, char[] senha) {
        validatePassword(senha);
        Usuario usuario = buscar(id);
        usuario.setSenhaHash(hasher.hash(senha));
        usuario.setAlterarSenha(false);
        usuario.setAtualizadoEm(LocalDateTime.now(clock));
        repository.atualizar(usuario);
        audit("TROCA_SENHA", id, null, "senha=alterada");
    }

    /** Retorna o usuario ou informa que ele nao existe. */
    public Usuario buscar(long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }

    /** Exige permissao administrativa e lista todas as contas. */
    public List<Usuario> listar() {
        return repository.listar();
    }

    /** Aplica o tamanho minimo da senha antes do hash. */
    private void validatePassword(char[] senha) {
        if (senha == null || senha.length < 8) {
            throw new ValidationException("A senha deve ter pelo menos 8 caracteres.");
        }
    }

    /** Compartilha a persistencia usada pelo cadastro e pelo administrador inicial. */
    private Usuario criarSemValidarSenha(
            String nome, String login, char[] senha,
            PerfilUsuario perfil, boolean alterarSenha) {
        LocalDateTime now = LocalDateTime.now(clock);
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setLogin(login);
        usuario.setSenhaHash(hasher.hash(senha));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuario.setAlterarSenha(alterarSenha);
        usuario.setCriadoEm(now);
        usuario.setAtualizadoEm(now);
        Usuario salvo = repository.salvar(usuario);
        audit("CRIACAO_USUARIO", salvo.getId(), null, safeValues(salvo));
        return salvo;
    }

    /** Remove espacos e rejeita texto obrigatorio vazio. */
    private String normalizeRequired(String value, String field) {
        String normalized = value == null ? "" : value.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) throw new ValidationException("O " + field + " é obrigatório.");
        return normalized;
    }

    /** Monta dados de auditoria sem incluir o hash da senha. */
    private String safeValues(Usuario usuario) {
        return "nome=" + usuario.getNome() + "; login=" + usuario.getLogin()
                + "; perfil=" + usuario.getPerfil() + "; ativo=" + usuario.isAtivo();
    }

    /** Registra criacao ou alteracao sem interromper a operacao principal. */
    private void audit(String action, Long id, String before, String after) {
        if (auditoria != null) {
            auditoria.registrar(action, "USUARIO", id, before, after);
        }
    }
}
