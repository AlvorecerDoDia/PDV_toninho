package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.PerfilUsuario;
import br.com.loja.pdv.domain.enums.Permissao;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.sqlite.SQLiteUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Testa regras de negocio sem depender da interface grafica. */
class UsuarioServiceTest {
    @TempDir Path tempDirectory;
    private UsuarioService usuarios;
    private AutenticacaoService autenticacao;
    private SessaoUsuario sessao;
    private SQLiteUsuarioRepository repository;

    @BeforeEach
    void setUp() {
        Database database = new Database(tempDirectory.resolve("usuarios.db"));
        new DatabaseInitializer(database).initialize();
        repository = new SQLiteUsuarioRepository(database);
        PasswordHasher hasher = new PasswordHasher();
        usuarios = new UsuarioService(repository, hasher);
        sessao = new SessaoUsuario();
        autenticacao = new AutenticacaoService(repository, hasher, sessao);
    }

    /** Verifica o cenario: deve criar administrador inicial com hash etroca obrigatoria. */
    @Test
    void deveCriarAdministradorInicialComHashETrocaObrigatoria() {
        Usuario admin = usuarios.criarAdministradorInicial("SenhaForte1".toCharArray());
        assertNotEquals("SenhaForte1", admin.getSenhaHash());
        assertTrue(admin.getSenhaHash().startsWith("pbkdf2-sha256$"));
        assertEquals(PerfilUsuario.ADMINISTRADOR, admin.getPerfil());
        assertTrue(admin.isAlterarSenha());
    }

    /** Verifica o cenario: deve configurar login esenha admin no primeiro acesso. */
    @Test
    void deveConfigurarLoginESenhaAdminNoPrimeiroAcesso() {
        assertTrue(usuarios.configurarAdministradorInicialPadrao());

        Usuario admin = autenticacao.autenticar("admin", "admin".toCharArray());

        assertEquals(PerfilUsuario.ADMINISTRADOR, admin.getPerfil());
        assertTrue(admin.isAlterarSenha());
        assertNotEquals("admin", admin.getSenhaHash());
    }

    /** Verifica o cenario: nao deve redefinir senha depois da troca obrigatoria. */
    @Test
    void naoDeveRedefinirSenhaDepoisDaTrocaObrigatoria() {
        usuarios.configurarAdministradorInicialPadrao();
        Usuario admin = autenticacao.autenticar("admin", "admin".toCharArray());
        usuarios.trocarSenha(admin.getId(), "NovaSenha2".toCharArray());

        assertFalse(usuarios.configurarAdministradorInicialPadrao());
        assertThrows(ValidationException.class, () ->
                autenticacao.autenticar("admin", "admin".toCharArray()));
        assertDoesNotThrow(() ->
                autenticacao.autenticar("admin", "NovaSenha2".toCharArray()));
    }

    /** Verifica o cenario: deve autenticar usuario valido emanter sessao. */
    @Test
    void deveAutenticarUsuarioValidoEManterSessao() {
        Usuario admin = usuarios.criarAdministradorInicial("SenhaForte1".toCharArray());
        Usuario logged = autenticacao.autenticar("ADMIN", "SenhaForte1".toCharArray());
        assertEquals(admin.getId(), logged.getId());
        assertEquals(admin.getId(), sessao.atual().orElseThrow().getId());
    }

    /** Verifica o cenario: deve recusar login invalido eusuario inativo. */
    @Test
    void deveRecusarLoginInvalidoEUsuarioInativo() {
        Usuario admin = usuarios.criarAdministradorInicial("SenhaForte1".toCharArray());
        assertThrows(ValidationException.class, () ->
                autenticacao.autenticar("admin", "incorreta".toCharArray()));
        usuarios.atualizar(admin.getId(), admin.getNome(), admin.getLogin(), admin.getPerfil(), false);
        assertThrows(ValidationException.class, () ->
                autenticacao.autenticar("admin", "SenhaForte1".toCharArray()));
    }

    /** Verifica o cenario: deve trocar senha eremover obrigatoriedade. */
    @Test
    void deveTrocarSenhaERemoverObrigatoriedade() {
        Usuario admin = usuarios.criarAdministradorInicial("SenhaForte1".toCharArray());
        usuarios.trocarSenha(admin.getId(), "NovaSenha2".toCharArray());
        assertFalse(usuarios.buscar(admin.getId()).isAlterarSenha());
        autenticacao.autenticar("admin", "NovaSenha2".toCharArray());
    }

    /** Verifica o cenario: deve bloquear funcao sem permissao. */
    @Test
    void deveBloquearFuncaoSemPermissao() {
        Usuario operador = usuarios.criar(
                "Operador", "operador", "SenhaForte1".toCharArray(),
                PerfilUsuario.OPERADOR, false);
        sessao.iniciar(operador);
        assertThrows(ValidationException.class, () -> sessao.exigir(Permissao.USUARIOS));
        assertDoesNotThrow(() -> sessao.exigir(Permissao.VENDAS));
    }

    /** Verifica o cenario: deve permitir desconto somente para perfil autorizado. */
    @Test
    void devePermitirDescontoSomenteParaPerfilAutorizado() {
        Usuario operador = usuarios.criar(
                "Operador", "operador", "SenhaForte1".toCharArray(),
                PerfilUsuario.OPERADOR, false);
        sessao.iniciar(operador);
        assertThrows(ValidationException.class, () -> sessao.exigir(Permissao.DESCONTOS));

        Usuario gerente = usuarios.criar(
                "Gerente", "gerente", "SenhaForte2".toCharArray(),
                PerfilUsuario.GERENTE, false);
        sessao.iniciar(gerente);
        assertDoesNotThrow(() -> sessao.exigir(Permissao.DESCONTOS));
    }

    /** Verifica o cenario: deve impedir segundo administrador inicial. */
    @Test
    void deveImpedirSegundoAdministradorInicial() {
        usuarios.criarAdministradorInicial("SenhaForte1".toCharArray());
        assertThrows(ValidationException.class, () ->
                usuarios.criarAdministradorInicial("OutraSenha2".toCharArray()));
        assertEquals(1, repository.contar());
    }
}
