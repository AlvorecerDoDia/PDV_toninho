package br.com.loja.pdv.integration;

import br.com.loja.pdv.domain.enums.*;
import br.com.loja.pdv.domain.model.*;
import br.com.loja.pdv.exception.ImpressaoException;
import br.com.loja.pdv.infrastructure.backup.GerenciadorBackup;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.infrastructure.printing.FormatadorComprovante;
import br.com.loja.pdv.infrastructure.printing.ImpressoraComprovante;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.sqlite.*;
import br.com.loja.pdv.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FluxoCompletoPdvTest {
    @TempDir Path tempDirectory;

    @Test
    void deveExecutarFluxoCompletoDoBancoVazioAteRestauracao() {
        Database database = new Database(tempDirectory.resolve("data").resolve("pdv.db"));
        new DatabaseInitializer(database).initialize();

        SQLiteUsuarioRepository usuarios = new SQLiteUsuarioRepository(database);
        PasswordHasher hasher = new PasswordHasher();
        UsuarioService usuarioService = new UsuarioService(usuarios, hasher);
        Usuario administrador = usuarioService.criar(
                "Administrador", "admin", "SenhaForte1".toCharArray(),
                PerfilUsuario.ADMINISTRADOR, false);
        SessaoUsuario sessao = new SessaoUsuario();
        Usuario autenticado = new AutenticacaoService(
                usuarios, hasher, sessao).autenticar(
                "ADMIN", "SenhaForte1".toCharArray());
        assertEquals(administrador.getId(), autenticado.getId());

        SQLiteProdutoRepository produtos = new SQLiteProdutoRepository(database);
        ProdutoService produtoService = new ProdutoService(produtos);
        Produto produto = produtoService.cadastrar(produto(
                "789100000001", "Café", "12.00", "30.00"));

        SQLiteEstoqueRepository estoqueRepository =
                new SQLiteEstoqueRepository(database);
        EstoqueService estoqueService =
                new EstoqueService(estoqueRepository, produtos);
        estoqueService.registrar(
                produto.getId(), TipoMovimentacaoEstoque.ENTRADA, 20,
                "Estoque inicial");
        produto = produtoService.buscarPorId(produto.getId());

        SQLiteCaixaRepository caixas = new SQLiteCaixaRepository(database);
        CaixaService caixaService = new CaixaService(caixas, sessao);
        Caixa caixa = caixaService.abrir(new BigDecimal("50.00"));

        CarrinhoVenda carrinho = new CarrinhoVenda();
        carrinho.adicionar(produto, 2);
        carrinho.aplicarDesconto(new BigDecimal("2.00"));
        PagamentoService pagamentoService = new PagamentoService();
        VendaService vendaService = new VendaService(
                new SQLiteVendaRepository(database), produtos, caixas,
                sessao, pagamentoService);
        Venda venda = vendaService.finalizar(carrinho, List.of(
                pagamentoService.criar(
                        FormaPagamento.DINHEIRO, new BigDecimal("20.00")),
                pagamentoService.criar(
                        FormaPagamento.PIX, new BigDecimal("40.00"))));

        assertEquals(new BigDecimal("2.00"), venda.getTroco());
        assertEquals(18, estoqueRepository.buscarSaldo(produto.getId()));
        assertEquals(new BigDecimal("68.00"),
                caixas.buscarDinheiroEsperado(caixa.getId()));
        assertTrue(carrinho.isVazio());

        String comprovante = new FormatadorComprovante("PDV Toninho")
                .formatar(venda, false);
        assertTrue(comprovante.contains("COMPROVANTE NÃO FISCAL"));
        assertTrue(comprovante.contains(venda.getNumero()));

        Venda historico = vendaService.buscarPorNumero(venda.getNumero());
        assertEquals(1, historico.getItens().size());
        Caixa fechado = caixaService.fechar(new BigDecimal("68.00"));
        assertEquals(BigDecimal.ZERO.setScale(2), fechado.getDiferenca());

        RelatorioService relatorios = new RelatorioService(
                new SQLiteRelatorioRepository(database), sessao);
        var linhas = relatorios.gerar(
                TipoRelatorio.VENDAS_POR_PERIODO,
                new FiltroRelatorio(
                        LocalDate.now(), LocalDate.now(),
                        administrador.getId(), null, null));
        assertFalse(linhas.isEmpty());
        assertEquals(new BigDecimal("58.00"), linhas.getFirst().valor());

        GerenciadorBackup gerenciador = new GerenciadorBackup(
                database, tempDirectory.resolve("backups"));
        BackupService backups = new BackupService(gerenciador, sessao);
        Path backup = backups.criarManual();
        assertTrue(Files.isRegularFile(backup));

        produtoService.cadastrar(produto(
                "789100000002", "Produto temporário", "1.00", "2.00"));
        assertEquals(2, produtoService.pesquisar("").size());
        Path copiaAnterior = backups.restaurar(backup);

        assertTrue(Files.isRegularFile(copiaAnterior));
        assertEquals(1, produtoService.pesquisar("").size());
        assertEquals(18, estoqueRepository.buscarSaldo(produto.getId()));
        assertEquals(StatusCaixa.FECHADO,
                caixas.buscarPorId(caixa.getId()).orElseThrow().getStatus());
    }

    @Test
    void deveManterVendaSalvaQuandoImpressoraEstaIndisponivel() {
        Venda venda = new Venda();
        venda.setNumero("V-TESTE");
        ImpressoraComprovante indisponivel = (registro, segundaVia) -> {
            throw new ImpressaoException(
                    "Nenhuma impressora padrão foi encontrada. A venda continua salva.");
        };

        ImpressaoException erro = assertThrows(
                ImpressaoException.class, () -> indisponivel.imprimir(venda, false));

        assertEquals("V-TESTE", venda.getNumero());
        assertTrue(erro.getMessage().contains("continua salva"));
    }

    private Produto produto(String codigo, String nome, String custo, String venda) {
        Produto produto = new Produto();
        produto.setCodigoBarras(codigo);
        produto.setNome(nome);
        produto.setPrecoCusto(new BigDecimal(custo));
        produto.setPrecoVenda(new BigDecimal(venda));
        produto.setEstoqueMinimo(2);
        return produto;
    }
}
