package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.exception.DuplicateBarcodeException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.repository.sqlite.SQLiteProdutoRepository;
import br.com.loja.pdv.service.ProdutoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteProdutoRepositoryTest {

    @TempDir
    Path tempDirectory;

    private Database database;
    private ProdutoService service;

    @BeforeEach
    void setUp() {
        database = new Database(tempDirectory.resolve("produtos.db"));
        new DatabaseInitializer(database).initialize();
        service = new ProdutoService(new SQLiteProdutoRepository(database));
    }

    @Test
    void deveCadastrarBuscarPorIdECodigo() {
        Produto novo = produto("789", "Café", "10.25", "15.90");
        novo.setQuantidadeEstoque(12);
        Produto saved = service.cadastrar(novo);

        assertTrue(saved.getId() > 0);
        assertEquals("Café", service.buscarPorId(saved.getId()).getNome());
        assertEquals(saved.getId(), service.buscarPorCodigoBarras("789").orElseThrow().getId());
        assertEquals(new BigDecimal("15.90"), saved.getPrecoVenda());
        assertEquals(12, service.buscarPorId(saved.getId()).getQuantidadeEstoque());
    }

    @Test
    void deveCadastrarSemCodigoDeBarras() {
        Produto saved = service.cadastrar(produto("  ", "Arroz", "5.00", "8.00"));

        assertNull(saved.getCodigoBarras());
        assertTrue(service.buscarPorCodigoBarras(" ").isEmpty());
    }

    @Test
    void deveImpedirCodigoDeBarrasDuplicado() {
        service.cadastrar(produto("123", "Produto A", "1.00", "2.00"));

        assertThrows(
                DuplicateBarcodeException.class,
                () -> service.cadastrar(produto("123", "Produto B", "2.00", "3.00"))
        );
    }

    @Test
    void devePesquisarPorNomeNormalizado() {
        service.cadastrar(produto(null, "Café Torrado", "10.00", "14.00"));
        service.cadastrar(produto(null, "Açúcar", "4.00", "6.00"));

        assertEquals(1, service.pesquisar("  café ").size());
    }

    @Test
    void deveAtualizarSemAlterarEstoqueOuCriacao() {
        Produto novo = produto("1", "Leite", "4.00", "6.00");
        novo.setQuantidadeEstoque(7);
        Produto saved = service.cadastrar(novo);
        saved.setNome("Leite Integral");
        saved.setPrecoVenda(new BigDecimal("6.50"));
        saved.setQuantidadeEstoque(999);

        service.atualizar(saved);
        Produto updated = service.buscarPorId(saved.getId());

        assertEquals("Leite Integral", updated.getNome());
        assertEquals(new BigDecimal("6.50"), updated.getPrecoVenda());
        assertEquals(7, updated.getQuantidadeEstoque());
    }

    @Test
    void deveDesativarEReativar() {
        Produto saved = service.cadastrar(produto(null, "Farinha", "3.00", "5.00"));

        service.desativar(saved.getId());
        assertFalse(service.buscarPorId(saved.getId()).isAtivo());
        assertTrue(service.listarAtivos().isEmpty());

        service.reativar(saved.getId());
        assertTrue(service.buscarPorId(saved.getId()).isAtivo());
    }

    @Test
    void devePersistirAposReabrirRepositorio() {
        Produto saved = service.cadastrar(produto("999", "Persistente", "1.23", "4.56"));

        ProdutoService reopened = new ProdutoService(new SQLiteProdutoRepository(database));

        assertEquals("Persistente", reopened.buscarPorId(saved.getId()).getNome());
        assertEquals(new BigDecimal("1.23"), reopened.buscarPorId(saved.getId()).getPrecoCusto());
    }

    private Produto produto(String barcode, String name, String cost, String price) {
        Produto produto = new Produto();
        produto.setCodigoBarras(barcode);
        produto.setNome(name);
        produto.setPrecoCusto(new BigDecimal(cost));
        produto.setPrecoVenda(new BigDecimal(price));
        produto.setEstoqueMinimo(2);
        return produto;
    }
}
