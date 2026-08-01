package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.PerfilUsuario;
import br.com.loja.pdv.domain.enums.StatusCaixa;
import br.com.loja.pdv.domain.model.Caixa;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.sqlite.SQLiteCaixaRepository;
import br.com.loja.pdv.repository.sqlite.SQLiteUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Testa regras de negocio sem depender da interface grafica. */
class CaixaServiceTest {
    @TempDir Path tempDirectory;
    private CaixaService service;
    private SQLiteCaixaRepository repository;

    @BeforeEach
    void setUp() {
        Database database = new Database(tempDirectory.resolve("caixa-service.db"));
        new DatabaseInitializer(database).initialize();
        SQLiteUsuarioRepository users = new SQLiteUsuarioRepository(database);
        Usuario operator = new UsuarioService(users, new PasswordHasher()).criar(
                "Operador", "operador", "SenhaForte1".toCharArray(),
                PerfilUsuario.OPERADOR, false);
        SessaoUsuario session = new SessaoUsuario();
        session.iniciar(operator);
        repository = new SQLiteCaixaRepository(database);
        service = new CaixaService(repository, session);
    }

    /** Verifica o cenario: deve abrir caixa. */
    @Test
    void deveAbrirCaixa() {
        Caixa caixa = service.abrir(new BigDecimal("50.00"));
        assertNotNull(caixa.getId());
        assertEquals(StatusCaixa.ABERTO, caixa.getStatus());
        assertEquals(new BigDecimal("50.00"), repository.buscarDinheiroEsperado(caixa.getId()));
    }

    /** Verifica o cenario: deve impedir abertura duplicada. */
    @Test
    void deveImpedirAberturaDuplicada() {
        service.abrir(BigDecimal.ZERO);
        assertThrows(ValidationException.class, () -> service.abrir(BigDecimal.TEN));
    }

    /** Verifica o cenario: deve registrar suprimento esangria. */
    @Test
    void deveRegistrarSuprimentoESangria() {
        Caixa caixa = service.abrir(new BigDecimal("100.00"));
        service.suprir(new BigDecimal("25.50"), "Troco adicional");
        service.sangrar(new BigDecimal("20.00"), "Retirada segura");
        assertEquals(new BigDecimal("105.50"),
                repository.buscarDinheiroEsperado(caixa.getId()));
        assertEquals(3, repository.listarMovimentacoes(caixa.getId()).size());
    }

    /** Verifica o cenario: deve impedir sangria maior que saldo. */
    @Test
    void deveImpedirSangriaMaiorQueSaldo() {
        service.abrir(new BigDecimal("10.00"));
        assertThrows(ValidationException.class, () ->
                service.sangrar(new BigDecimal("10.01"), "Retirada"));
    }

    /** Verifica o cenario: deve exigir motivo evalor valido. */
    @Test
    void deveExigirMotivoEValorValido() {
        service.abrir(BigDecimal.ZERO);
        assertThrows(ValidationException.class, () ->
                service.suprir(BigDecimal.TEN, " "));
        assertThrows(ValidationException.class, () ->
                service.suprir(new BigDecimal("1.001"), "Troco"));
        assertThrows(ValidationException.class, () ->
                service.sangrar(BigDecimal.ZERO, "Retirada"));
    }

    /** Verifica o cenario: deve fechar caixa ecalcular diferenca. */
    @Test
    void deveFecharCaixaECalcularDiferenca() {
        Caixa open = service.abrir(new BigDecimal("100.00"));
        service.suprir(new BigDecimal("20.00"), "Troco");
        Caixa closed = service.fechar(new BigDecimal("118.50"));
        assertEquals(open.getId(), closed.getId());
        assertEquals(StatusCaixa.FECHADO, closed.getStatus());
        assertEquals(new BigDecimal("120.00"), closed.getValorEsperado());
        assertEquals(new BigDecimal("118.50"), closed.getValorContado());
        assertEquals(new BigDecimal("-1.50"), closed.getDiferenca());
    }

    /** Verifica o cenario: deve associar caixa ao usuario da sessao. */
    @Test
    void deveAssociarCaixaAoUsuarioDaSessao() {
        Caixa caixa = service.abrir(BigDecimal.ZERO);
        assertEquals(caixa.getUsuarioId(),
                repository.buscarAbertoPorUsuario(caixa.getUsuarioId()).orElseThrow().getUsuarioId());
    }
}
