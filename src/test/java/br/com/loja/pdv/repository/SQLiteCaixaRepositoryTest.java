package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.enums.PerfilUsuario;
import br.com.loja.pdv.domain.enums.StatusCaixa;
import br.com.loja.pdv.domain.enums.TipoMovimentacaoCaixa;
import br.com.loja.pdv.domain.model.Caixa;
import br.com.loja.pdv.domain.model.MovimentacaoCaixa;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.sqlite.SQLiteCaixaRepository;
import br.com.loja.pdv.repository.sqlite.SQLiteUsuarioRepository;
import br.com.loja.pdv.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteCaixaRepositoryTest {
    @TempDir Path tempDirectory;
    private Database database;
    private SQLiteCaixaRepository repository;
    private Usuario operator;

    @BeforeEach
    void setUp() {
        database = new Database(tempDirectory.resolve("caixa-repository.db"));
        new DatabaseInitializer(database).initialize();
        SQLiteUsuarioRepository users = new SQLiteUsuarioRepository(database);
        operator = new UsuarioService(users, new PasswordHasher()).criar(
                "Operador", "operador", "SenhaForte1".toCharArray(),
                PerfilUsuario.OPERADOR, false);
        repository = new SQLiteCaixaRepository(database);
    }

    @Test
    void devePersistirCaixaEMovimentacao() {
        Caixa caixa = open(new BigDecimal("40.00"));
        SQLiteCaixaRepository reopened = new SQLiteCaixaRepository(database);
        assertEquals(caixa.getId(),
                reopened.buscarAbertoPorUsuario(operator.getId()).orElseThrow().getId());
        assertEquals(new BigDecimal("40.00"), reopened.buscarDinheiroEsperado(caixa.getId()));
        assertEquals(1, reopened.listarMovimentacoes(caixa.getId()).size());
    }

    @Test
    void deveImpedirMovimentacaoEmCaixaFechado() {
        Caixa caixa = open(BigDecimal.TEN);
        repository.fechar(caixa.getId(), BigDecimal.TEN, LocalDateTime.now());
        assertThrows(ValidationException.class, () ->
                repository.registrar(movement(caixa, TipoMovimentacaoCaixa.SUPRIMENTO,
                        BigDecimal.ONE, "Troco")));
    }

    @Test
    void deveFazerRollbackDaAberturaQuandoMovimentacaoFalhar() throws Exception {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TRIGGER falha_caixa BEFORE INSERT ON movimentacao_caixa
                    BEGIN SELECT RAISE(ABORT, 'falha provocada'); END
                    """);
        }
        assertThrows(DatabaseException.class, () -> open(BigDecimal.TEN));
        assertTrue(repository.buscarAbertoPorUsuario(operator.getId()).isEmpty());
    }

    private Caixa open(BigDecimal value) {
        LocalDateTime now = LocalDateTime.now();
        Caixa caixa = new Caixa();
        caixa.setUsuarioId(operator.getId());
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setValorAbertura(value);
        caixa.setAbertoEm(now);
        return repository.abrir(
                caixa, movement(caixa, TipoMovimentacaoCaixa.ABERTURA, value, "Abertura"));
    }

    private MovimentacaoCaixa movement(
            Caixa caixa, TipoMovimentacaoCaixa type, BigDecimal value, String reason) {
        MovimentacaoCaixa movement = new MovimentacaoCaixa();
        if (caixa.getId() != null) movement.setCaixaId(caixa.getId());
        movement.setUsuarioId(operator.getId());
        movement.setTipo(type);
        movement.setValor(value);
        movement.setMotivo(reason);
        movement.setCriadoEm(LocalDateTime.now());
        return movement;
    }
}
