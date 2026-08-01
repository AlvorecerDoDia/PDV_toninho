package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.enums.*;
import br.com.loja.pdv.domain.model.*;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.infrastructure.reporting.ExportadorCsv;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.sqlite.*;
import br.com.loja.pdv.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Testa a persistencia SQLite usando um banco temporario e isolado. */
class SQLiteRelatorioRepositoryTest {
    @TempDir Path tempDirectory;
    private RelatorioService reports;
    private FiltroRelatorio filter;
    private Produto product;
    private Usuario manager;

    @BeforeEach
    void setUp() {
        Database database = new Database(tempDirectory.resolve("relatorios.db"));
        new DatabaseInitializer(database).initialize();
        SQLiteUsuarioRepository users = new SQLiteUsuarioRepository(database);
        manager = new UsuarioService(users, new PasswordHasher()).criar(
                "Gerente", "gerente", "SenhaForte1".toCharArray(),
                PerfilUsuario.GERENTE, false);
        SessaoUsuario session = new SessaoUsuario();
        session.iniciar(manager);
        SQLiteProdutoRepository products = new SQLiteProdutoRepository(database);
        product = new Produto();
        product.setNome("Café");
        product.setPrecoCusto(new BigDecimal("30.00"));
        product.setPrecoVenda(new BigDecimal("100.00"));
        product.setEstoqueMinimo(8);
        product = new ProdutoService(products).cadastrar(product);
        SQLiteEstoqueRepository stock = new SQLiteEstoqueRepository(database);
        new EstoqueService(stock, products).registrar(
                product.getId(), TipoMovimentacaoEstoque.ENTRADA, 10, null);
        SQLiteCaixaRepository cash = new SQLiteCaixaRepository(database);
        CaixaService cashService = new CaixaService(cash, session);
        cashService.abrir(BigDecimal.ZERO);
        PagamentoService paymentService = new PagamentoService();
        VendaService saleService = new VendaService(
                new SQLiteVendaRepository(database), products, cash, session, paymentService);

        CarrinhoVenda firstCart = new CarrinhoVenda();
        firstCart.adicionar(products.buscarPorId(product.getId()).orElseThrow(), 2);
        firstCart.aplicarDesconto(new BigDecimal("10.00"));
        saleService.finalizar(firstCart, List.of(paymentService.criar(
                FormaPagamento.DINHEIRO, new BigDecimal("190.00"))));

        CarrinhoVenda canceledCart = new CarrinhoVenda();
        canceledCart.adicionar(products.buscarPorId(product.getId()).orElseThrow(), 1);
        Venda canceled = saleService.finalizar(canceledCart, List.of(paymentService.criar(
                FormaPagamento.PIX, new BigDecimal("100.00"))));
        saleService.cancelar(canceled.getId(), "Venda de teste cancelada");
        cashService.fechar(new BigDecimal("190.00"));

        reports = new RelatorioService(
                new SQLiteRelatorioRepository(database), session);
        filter = new FiltroRelatorio(
                LocalDate.now(), LocalDate.now(), null, null, null);
    }

    /** Verifica o cenario: deve gerar vendas totais pagamentos eprodutos sem canceladas. */
    @Test
    void deveGerarVendasTotaisPagamentosEProdutosSemCanceladas() {
        LinhaRelatorio daily = reports.gerar(
                TipoRelatorio.VENDAS_POR_DIA, filter).getFirst();
        assertEquals(1L, daily.quantidade());
        assertEquals(new BigDecimal("190.00"), daily.valor());

        LinhaRelatorio payment = reports.gerar(
                TipoRelatorio.TOTAL_POR_FORMA_PAGAMENTO, filter).getFirst();
        assertEquals("DINHEIRO", payment.categoria());
        assertEquals(new BigDecimal("190.00"), payment.valor());

        LinhaRelatorio productLine = reports.gerar(
                TipoRelatorio.PRODUTOS_MAIS_VENDIDOS, filter).getFirst();
        assertEquals(2L, productLine.quantidade());
        assertEquals(new BigDecimal("200.00"), productLine.valor());
        assertEquals(new BigDecimal("60.00"), productLine.valorSecundario());
    }

    /** Verifica o cenario: deve gerar filtros descontos cancelamentos estoque ecaixa. */
    @Test
    void deveGerarFiltrosDescontosCancelamentosEstoqueECaixa() {
        FiltroRelatorio managerFilter = new FiltroRelatorio(
                filter.inicio(), filter.fim(), manager.getId(),
                FormaPagamento.DINHEIRO, product.getId());
        assertEquals(1, reports.gerar(
                TipoRelatorio.VENDAS_POR_OPERADOR, managerFilter).size());
        assertEquals(new BigDecimal("10.00"), reports.gerar(
                TipoRelatorio.DESCONTOS, managerFilter).getFirst().valor());
        assertEquals(new BigDecimal("100.00"), reports.gerar(
                TipoRelatorio.CANCELAMENTOS, managerFilter).getFirst().valor());
        assertEquals(8L, reports.gerar(
                TipoRelatorio.ESTOQUE_BAIXO, managerFilter).getFirst().quantidade());
        LinhaRelatorio closure = reports.gerar(
                TipoRelatorio.FECHAMENTO_CAIXA, managerFilter).getFirst();
        assertEquals(new BigDecimal("190.00"), closure.valor());
        assertEquals(new BigDecimal("190.00"), closure.valorSecundario());
    }

    /** Verifica o cenario: deve calcular lucro com custo historico edesconsiderar canceladas. */
    @Test
    void deveCalcularLucroComCustoHistoricoEDesconsiderarCanceladas() {
        LinhaRelatorio profit = reports.gerar(
                TipoRelatorio.LUCRO_BRUTO_ESTIMADO, filter).getFirst();
        assertEquals(new BigDecimal("130.00"), profit.valor());
        assertFalse(reports.gerar(
                TipoRelatorio.MOVIMENTACOES_ESTOQUE, filter).isEmpty());
    }

    /** Verifica o cenario: deve exportar csv brasileiro seguro. */
    @Test
    void deveExportarCsvBrasileiroSeguro() throws Exception {
        Path csv = tempDirectory.resolve("saida").resolve("relatorio.csv");
        List<LinhaRelatorio> rows = List.of(new LinhaRelatorio(
                "=FÓRMULA", "Detalhe; com separador", 2L,
                new BigDecimal("1234.56"), null, null));
        new ExportadorCsv().exportar(csv, TipoRelatorio.VENDAS_POR_DIA, rows);
        String content = Files.readString(csv, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("\ufeff"));
        assertTrue(content.contains("1.234,56"));
        assertTrue(content.contains("'=FÓRMULA"));
        assertTrue(content.contains("\"Detalhe; com separador\""));
    }
}
