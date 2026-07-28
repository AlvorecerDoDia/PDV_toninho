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

    @Test
    void deveCriarAdministradorInicialComHashETrocaObrigatoria() {
        Usuario admin = usuarios.criarAdministradorInicial("SenhaForte1".toCharArray());
        assertNotEquals("SenhaForte1", admin.getSenhaHash());
        assertTrue(admin.getSenhaHash().startsWith("pbkdf2-sha256$"));
        assertEquals(PerfilUsuario.ADMINISTRADOR, admin.getPerfil());
        assertTrue(admin.isAlterarSenha());
    }

    @Test
    void deveConfigurarLoginESenhaAdminNoPrimeiroAcesso() {
        assertTrue(usuarios.configurarAdministradorInicialPadrao());

        Usuario admin = autenticacao.autenticar("admin", "admin".toCharArray());

        assertEquals(PerfilUsuario.ADMINISTRADOR, admin.getPerfil());
        assertTrue(admin.isAlterarSenha());
        assertNotEquals("admin", admin.getSenhaHash());
    }

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

    @Test
    void deveAutenticarUsuarioValidoEManterSessao() {
        Usuario admin = usuarios.criarAdministradorInicial("SenhaForte1".toCharArray());
        Usuario logged = autenticacao.autenticar("ADMIN", "SenhaForte1".toCharArray());
        assertEquals(admin.getId(), logged.getId());
        assertEquals(admin.getId(), sessao.atual().orElseThrow().getId());
    }

    @Test
    void deveRecusarLoginInvalidoEUsuarioInativo() {
        Usuario admin = usuarios.criarAdministradorInicial("SenhaForte1".toCharArray());
        assertThrows(ValidationException.class, () ->
                autenticacao.autenticar("admin", "incorreta".toCharArray()));
        usuarios.atualizar(admin.getId(), admin.getNome(), admin.getLogin(), admin.getPerfil(), false);
        assertThrows(ValidationException.class, () ->
                autenticacao.autenticar("admin", "SenhaForte1".toCharArray()));
    }

    @Test
    void deveTrocarSenhaERemoverObrigatoriedade() {
        Usuario admin = usuarios.criarAdministradorInicial("SenhaForte1".toCharArray());
        usuarios.trocarSenha(admin.getId(), "NovaSenha2".toCharArray());
        assertFalse(usuarios.buscar(admin.getId()).isAlterarSenha());
        autenticacao.autenticar("admin", "NovaSenha2".toCharArray());
    }

    @Test
    void deveBloquearFuncaoSemPermissao() {
        Usuario operador = usuarios.criar(
                "Operador", "operador", "SenhaForte1".toCharArray(),
                PerfilUsuario.OPERADOR, false);
        sessao.iniciar(operador);
        assertThrows(ValidationException.class, () -> sessao.exigir(Permissao.USUARIOS));
        assertDoesNotThrow(() -> sessao.exigir(Permissao.VENDAS));
    }

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

    @Test
    void deveImpedirSegundoAdministradorInicial() {
        usuarios.criarAdministradorInicial("SenhaForte1".toCharArray());
        assertThrows(ValidationException.class, () ->
                usuarios.criarAdministradorInicial("OutraSenha2".toCharArray()));
        assertEquals(1, repository.contar());
    }
}
