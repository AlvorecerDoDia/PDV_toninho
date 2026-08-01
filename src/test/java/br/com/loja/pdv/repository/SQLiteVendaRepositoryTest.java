package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.enums.FormaPagamento;
import br.com.loja.pdv.domain.enums.PerfilUsuario;
import br.com.loja.pdv.domain.enums.TipoMovimentacaoEstoque;
import br.com.loja.pdv.domain.model.CarrinhoVenda;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.domain.model.Venda;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.sqlite.*;
import br.com.loja.pdv.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Testa a persistencia SQLite usando um banco temporario e isolado. */
class SQLiteVendaRepositoryTest {
    @TempDir Path tempDirectory;
    private Database database;
    private SQLiteProdutoRepository products;
    private SQLiteEstoqueRepository stock;
    private SQLiteCaixaRepository cashRegisters;
    private SQLiteVendaRepository sales;
    private SQLitePagamentoRepository payments;
    private PagamentoService paymentService;
    private VendaService saleService;
    private CaixaService cashService;
    private Produto product;
    private SQLiteUsuarioRepository users;
    private Usuario manager;

    @BeforeEach
    void setUp() {
        database = new Database(tempDirectory.resolve("vendas.db"));
        new DatabaseInitializer(database).initialize();
        products = new SQLiteProdutoRepository(database);
        stock = new SQLiteEstoqueRepository(database);
        cashRegisters = new SQLiteCaixaRepository(database);
        sales = new SQLiteVendaRepository(database);
        payments = new SQLitePagamentoRepository(database);
        users = new SQLiteUsuarioRepository(database);
        manager = new UsuarioService(users, new PasswordHasher()).criar(
                "Gerente", "gerente", "SenhaForte1".toCharArray(),
                PerfilUsuario.GERENTE, false);
        SessaoUsuario session = new SessaoUsuario();
        session.iniciar(manager);
        paymentService = new PagamentoService();
        saleService = new VendaService(
                sales, products, cashRegisters, session, paymentService);
        cashService = new CaixaService(cashRegisters, session);
        product = createProduct("Produto", "30.00", "100.00");
        new EstoqueService(stock, products).registrar(
                product.getId(), TipoMovimentacaoEstoque.ENTRADA, 10, null);
        product = products.buscarPorId(product.getId()).orElseThrow();
    }

    /** Verifica o cenario: deve finalizar venda em dinheiro calcular troco eatualizar caixa. */
    @Test
    void deveFinalizarVendaEmDinheiroCalcularTrocoEAtualizarCaixa() {
        var cash = cashService.abrir(BigDecimal.ZERO);
        CarrinhoVenda cart = cart(1);
        Venda sale = saleService.finalizar(cart, List.of(
                paymentService.criar(FormaPagamento.DINHEIRO, new BigDecimal("120.00"))));

        assertNotNull(sale.getId());
        assertEquals(new BigDecimal("20.00"), sale.getTroco());
        assertEquals(9, stock.buscarSaldo(product.getId()));
        assertEquals(new BigDecimal("100.00"),
                cashRegisters.buscarDinheiroEsperado(cash.getId()));
        assertTrue(cart.isVazio());
    }

    /** Verifica o cenario: deve finalizar venda pix sem movimentar dinheiro do caixa. */
    @Test
    void deveFinalizarVendaPixSemMovimentarDinheiroDoCaixa() {
        var cash = cashService.abrir(new BigDecimal("10.00"));
        Venda sale = saleService.finalizar(cart(1), List.of(
                paymentService.criar(FormaPagamento.PIX, new BigDecimal("100.00"))));
        assertNotNull(sale.getId());
        assertEquals(new BigDecimal("10.00"),
                cashRegisters.buscarDinheiroEsperado(cash.getId()));
    }

    /** Verifica o cenario: deve finalizar pagamento combinado com troco somente do dinheiro. */
    @Test
    void deveFinalizarPagamentoCombinadoComTrocoSomenteDoDinheiro() {
        var cash = cashService.abrir(BigDecimal.ZERO);
        Venda sale = saleService.finalizar(cart(1), List.of(
                paymentService.criar(FormaPagamento.DINHEIRO, new BigDecimal("50.00")),
                paymentService.criar(FormaPagamento.PIX, new BigDecimal("60.00"))));
        assertEquals(new BigDecimal("10.00"), sale.getTroco());
        assertEquals(new BigDecimal("40.00"),
                cashRegisters.buscarDinheiroEsperado(cash.getId()));
    }

    /** Verifica o cenario: deve persistir itens pagamentos ecusto historico. */
    @Test
    void devePersistirItensPagamentosECustoHistorico() {
        cashService.abrir(BigDecimal.ZERO);
        Venda sale = saleService.finalizar(cart(2), List.of(
                paymentService.criar(
                        FormaPagamento.CARTAO_CREDITO, new BigDecimal("200.00"))));

        var persisted = sales.buscarPorId(sale.getId()).orElseThrow();
        var items = sales.listarItens(sale.getId());
        var persistedPayments = payments.listarPorVenda(sale.getId());
        assertEquals(sale.getNumero(), persisted.getNumero());
        assertEquals(1, items.size());
        assertEquals(2, items.getFirst().getQuantidade());
        assertEquals(new BigDecimal("30.00"), items.getFirst().getCustoUnitario());
        assertEquals(FormaPagamento.CARTAO_CREDITO,
                persistedPayments.getFirst().getForma());
    }

    /** Verifica o cenario: deve auditar desconto na mesma transacao da venda. */
    @Test
    void deveAuditarDescontoNaMesmaTransacaoDaVenda() throws Exception {
        cashService.abrir(BigDecimal.ZERO);
        CarrinhoVenda cart = cart(1);
        cart.aplicarDesconto(new BigDecimal("10.00"));

        Venda sale = saleService.finalizar(cart, List.of(
                paymentService.criar(FormaPagamento.PIX, new BigDecimal("90.00"))));

        assertEquals(1, count("auditoria"));
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT acao, entidade_id FROM auditoria")) {
            assertTrue(resultSet.next());
            assertEquals("DESCONTO", resultSet.getString("acao"));
            assertEquals(sale.getId(), resultSet.getLong("entidade_id"));
        }
    }

    /** Verifica o cenario: deve impedir venda sem caixa aberto. */
    @Test
    void deveImpedirVendaSemCaixaAberto() {
        CarrinhoVenda cart = cart(1);
        assertThrows(ValidationException.class, () ->
                saleService.finalizar(cart, List.of(
                        paymentService.criar(
                                FormaPagamento.DINHEIRO, new BigDecimal("100.00")))));
        assertFalse(cart.isVazio());
    }

    /** Verifica o cenario: deve impedir venda vazia. */
    @Test
    void deveImpedirVendaVazia() {
        cashService.abrir(BigDecimal.ZERO);
        assertThrows(ValidationException.class, () ->
                saleService.finalizar(new CarrinhoVenda(), List.of()));
    }

    /** Verifica o cenario: deve verificar estoque novamente na finalizacao. */
    @Test
    void deveVerificarEstoqueNovamenteNaFinalizacao() {
        cashService.abrir(BigDecimal.ZERO);
        CarrinhoVenda cart = cart(10);
        new EstoqueService(stock, products).registrar(
                product.getId(), TipoMovimentacaoEstoque.PERDA, 1, "Avaria");
        assertThrows(ValidationException.class, () ->
                saleService.finalizar(cart, List.of(
                        paymentService.criar(
                                FormaPagamento.DINHEIRO, new BigDecimal("1000.00")))));
        assertFalse(cart.isVazio());
    }

    /** Verifica o cenario: deve fazer rollback total quando pagamento falhar. */
    @Test
    void deveFazerRollbackTotalQuandoPagamentoFalhar() throws Exception {
        var cash = cashService.abrir(BigDecimal.ZERO);
        CarrinhoVenda cart = cart(1);
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TRIGGER falha_pagamento BEFORE INSERT ON pagamento
                    BEGIN SELECT RAISE(ABORT, 'falha provocada'); END
                    """);
        }

        assertThrows(DatabaseException.class, () ->
                saleService.finalizar(cart, List.of(
                        paymentService.criar(
                                FormaPagamento.DINHEIRO, new BigDecimal("100.00")))));
        assertEquals(10, stock.buscarSaldo(product.getId()));
        assertEquals(BigDecimal.ZERO.setScale(2),
                cashRegisters.buscarDinheiroEsperado(cash.getId()));
        assertEquals(0, count("venda"));
        assertEquals(0, count("item_venda"));
        assertFalse(cart.isVazio());
    }

    /** Verifica o cenario: deve consultar venda por numero periodo eoperador. */
    @Test
    void deveConsultarVendaPorNumeroPeriodoEOperador() {
        cashService.abrir(BigDecimal.ZERO);
        Venda sale = saleService.finalizar(cart(1), List.of(
                paymentService.criar(FormaPagamento.PIX, new BigDecimal("100.00"))));

        assertEquals(sale.getId(),
                saleService.buscarPorNumero(sale.getNumero().toLowerCase()).getId());
        assertEquals(1, saleService.listar(
                LocalDate.now(), LocalDate.now(), manager.getId()).size());
        assertTrue(saleService.listar(
                LocalDate.now(), LocalDate.now(), manager.getId() + 999).isEmpty());
    }

    /** Verifica o cenario: deve cancelar venda devolver estoque estornar caixa eauditar. */
    @Test
    void deveCancelarVendaDevolverEstoqueEstornarCaixaEAuditar() throws Exception {
        var cash = cashService.abrir(BigDecimal.ZERO);
        Venda sale = saleService.finalizar(cart(2), List.of(
                paymentService.criar(
                        FormaPagamento.DINHEIRO, new BigDecimal("200.00"))));

        Venda canceled = saleService.cancelar(sale.getId(), "Cliente desistiu");

        assertEquals(br.com.loja.pdv.domain.enums.StatusVenda.CANCELADA,
                canceled.getStatus());
        assertEquals(10, stock.buscarSaldo(product.getId()));
        assertEquals(BigDecimal.ZERO.setScale(2),
                cashRegisters.buscarDinheiroEsperado(cash.getId()));
        assertEquals(1, count("auditoria"));
        assertEquals(3, new EstoqueService(stock, products).listar(
                product.getId(), LocalDate.now(), LocalDate.now()).size());
    }

    /** Verifica o cenario: deve impedir cancelamento duplicado emotivo vazio. */
    @Test
    void deveImpedirCancelamentoDuplicadoEMotivoVazio() {
        cashService.abrir(BigDecimal.ZERO);
        Venda sale = saleService.finalizar(cart(1), List.of(
                paymentService.criar(FormaPagamento.PIX, new BigDecimal("100.00"))));
        assertThrows(ValidationException.class, () ->
                saleService.cancelar(sale.getId(), " "));
        saleService.cancelar(sale.getId(), "Erro no pedido");
        assertThrows(ValidationException.class, () ->
                saleService.cancelar(sale.getId(), "Nova tentativa"));
    }

    /** Verifica o cenario: deve bloquear cancelamento sem permissao. */
    @Test
    void deveBloquearCancelamentoSemPermissao() {
        cashService.abrir(BigDecimal.ZERO);
        Venda sale = saleService.finalizar(cart(1), List.of(
                paymentService.criar(FormaPagamento.PIX, new BigDecimal("100.00"))));
        Usuario operator = new UsuarioService(users, new PasswordHasher()).criar(
                "Operador", "operador", "SenhaForte2".toCharArray(),
                PerfilUsuario.OPERADOR, false);
        SessaoUsuario operatorSession = new SessaoUsuario();
        operatorSession.iniciar(operator);
        VendaService operatorSales = new VendaService(
                sales, products, cashRegisters, operatorSession, paymentService);
        assertThrows(ValidationException.class, () ->
                operatorSales.cancelar(sale.getId(), "Sem permissão"));
    }

    /** Verifica o cenario: deve recalcular diferenca ao cancelar venda de caixa fechado. */
    @Test
    void deveRecalcularDiferencaAoCancelarVendaDeCaixaFechado() {
        var cash = cashService.abrir(BigDecimal.ZERO);
        Venda sale = saleService.finalizar(cart(1), List.of(
                paymentService.criar(
                        FormaPagamento.DINHEIRO, new BigDecimal("100.00"))));
        cashService.fechar(new BigDecimal("100.00"));

        saleService.cancelar(sale.getId(), "Devolução posterior");

        var closed = cashRegisters.buscarPorId(cash.getId()).orElseThrow();
        assertEquals(BigDecimal.ZERO.setScale(2), closed.getValorEsperado());
        assertEquals(new BigDecimal("100.00"), closed.getDiferenca());
    }

    /** Verifica o cenario: deve fazer rollback do cancelamento quando auditoria falhar. */
    @Test
    void deveFazerRollbackDoCancelamentoQuandoAuditoriaFalhar() throws Exception {
        var cash = cashService.abrir(BigDecimal.ZERO);
        Venda sale = saleService.finalizar(cart(1), List.of(
                paymentService.criar(
                        FormaPagamento.DINHEIRO, new BigDecimal("100.00"))));
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TRIGGER falha_auditoria BEFORE INSERT ON auditoria
                    BEGIN SELECT RAISE(ABORT, 'falha provocada'); END
                    """);
        }

        assertThrows(DatabaseException.class, () ->
                saleService.cancelar(sale.getId(), "Teste de rollback"));
        assertEquals(br.com.loja.pdv.domain.enums.StatusVenda.FINALIZADA,
                sales.buscarPorId(sale.getId()).orElseThrow().getStatus());
        assertEquals(9, stock.buscarSaldo(product.getId()));
        assertEquals(new BigDecimal("100.00"),
                cashRegisters.buscarDinheiroEsperado(cash.getId()));
        assertEquals(0, count("auditoria"));
    }

    private CarrinhoVenda cart(int quantity) {
        CarrinhoVenda cart = new CarrinhoVenda();
        cart.adicionar(products.buscarPorId(product.getId()).orElseThrow(), quantity);
        return cart;
    }

    private Produto createProduct(String name, String cost, String price) {
        Produto value = new Produto();
        value.setNome(name);
        value.setPrecoCusto(new BigDecimal(cost));
        value.setPrecoVenda(new BigDecimal(price));
        value.setEstoqueMinimo(0);
        return new ProdutoService(products).cadastrar(value);
    }

    private int count(String table) throws Exception {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
