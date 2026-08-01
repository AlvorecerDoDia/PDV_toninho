package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.enums.TipoMovimentacaoEstoque;
import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.repository.sqlite.SQLiteEstoqueRepository;
import br.com.loja.pdv.repository.sqlite.SQLiteProdutoRepository;
import br.com.loja.pdv.service.EstoqueService;
import br.com.loja.pdv.service.ProdutoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Testa a persistencia SQLite usando um banco temporario e isolado. */
class SQLiteEstoqueRepositoryTest {
    @TempDir Path tempDirectory;
    private Database database;
    private Produto produto;
    private EstoqueService service;

    @BeforeEach
    void setUp() {
        database = new Database(tempDirectory.resolve("estoque.db"));
        new DatabaseInitializer(database).initialize();
        SQLiteProdutoRepository products = new SQLiteProdutoRepository(database);
        produto = new Produto();
        produto.setNome("Produto");
        produto.setPrecoCusto(new BigDecimal("1.00"));
        produto.setPrecoVenda(new BigDecimal("2.00"));
        produto.setEstoqueMinimo(2);
        new ProdutoService(products).cadastrar(produto);
        service = new EstoqueService(new SQLiteEstoqueRepository(database), products);
    }

    /** Verifica o cenario: deve registrar entrada eajuste positivo. */
    @Test
    void deveRegistrarEntradaEAjustePositivo() {
        service.registrar(produto.getId(), TipoMovimentacaoEstoque.ENTRADA, 10, null);
        service.registrar(produto.getId(), TipoMovimentacaoEstoque.AJUSTE_POSITIVO, 3, "Contagem");
        assertEquals(13, service.buscarSaldo(produto.getId()));
    }

    /** Verifica o cenario: deve registrar ajuste negativo eperda. */
    @Test
    void deveRegistrarAjusteNegativoEPerda() {
        service.registrar(produto.getId(), TipoMovimentacaoEstoque.ENTRADA, 10, null);
        service.registrar(produto.getId(), TipoMovimentacaoEstoque.AJUSTE_NEGATIVO, 2, "Contagem");
        service.registrar(produto.getId(), TipoMovimentacaoEstoque.PERDA, 1, "Avaria");
        assertEquals(7, service.buscarSaldo(produto.getId()));
    }

    /** Verifica o cenario: deve impedir estoque negativo. */
    @Test
    void deveImpedirEstoqueNegativo() {
        assertThrows(ValidationException.class, () ->
                service.registrar(produto.getId(), TipoMovimentacaoEstoque.PERDA, 1, "Avaria"));
        assertEquals(0, service.buscarSaldo(produto.getId()));
    }

    /** Verifica o cenario: deve exigir motivo para ajustes eperda. */
    @Test
    void deveExigirMotivoParaAjustesEPerda() {
        assertThrows(ValidationException.class, () -> service.registrar(
                produto.getId(), TipoMovimentacaoEstoque.AJUSTE_POSITIVO, 1, " "));
    }

    /** Verifica o cenario: deve listar historico com saldos anterior eposterior. */
    @Test
    void deveListarHistoricoComSaldosAnteriorEPosterior() {
        service.registrar(produto.getId(), TipoMovimentacaoEstoque.ENTRADA, 5, null);
        var history = service.listar(produto.getId(), LocalDate.now(), LocalDate.now());
        assertEquals(1, history.size());
        assertEquals(0, history.getFirst().getQuantidadeAnterior());
        assertEquals(5, history.getFirst().getQuantidadePosterior());
    }

    /** Verifica o cenario: deve fazer rollback quando historico falhar. */
    @Test
    void deveFazerRollbackQuandoHistoricoFalhar() throws Exception {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TRIGGER falha_movimentacao BEFORE INSERT ON movimentacao_estoque
                    BEGIN SELECT RAISE(ABORT, 'falha provocada'); END
                    """);
        }
        assertThrows(DatabaseException.class, () ->
                service.registrar(produto.getId(), TipoMovimentacaoEstoque.ENTRADA, 5, null));
        assertEquals(0, service.buscarSaldo(produto.getId()));
    }

    /** Verifica o cenario: deve impedir movimentacao de produto inativo. */
    @Test
    void deveImpedirMovimentacaoDeProdutoInativo() {
        new ProdutoService(new SQLiteProdutoRepository(database)).desativar(produto.getId());
        assertThrows(ValidationException.class, () ->
                service.registrar(produto.getId(), TipoMovimentacaoEstoque.ENTRADA, 1, null));
    }
}
