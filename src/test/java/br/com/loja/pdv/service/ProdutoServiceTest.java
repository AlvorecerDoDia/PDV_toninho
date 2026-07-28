package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProdutoServiceTest {

    private final ProdutoService service = new ProdutoService(new CapturingRepository());

    @Test
    void deveNormalizarNomeEEstoqueNoCadastro() {
        Produto produto = validProduct();
        produto.setNome("  Café   Especial  ");
        produto.setQuantidadeEstoque(50);

        Produto saved = service.cadastrar(produto);

        assertEquals("Café Especial", saved.getNome());
        assertEquals(0, saved.getQuantidadeEstoque());
    }

    @Test
    void deveImpedirNomeVazio() {
        Produto produto = validProduct();
        produto.setNome(" ");

        assertThrows(ValidationException.class, () -> service.cadastrar(produto));
    }

    @Test
    void deveImpedirPrecosNegativos() {
        Produto produto = validProduct();
        produto.setPrecoVenda(new BigDecimal("-0.01"));

        assertThrows(ValidationException.class, () -> service.cadastrar(produto));
    }

    @Test
    void deveImpedirEstoqueMinimoNegativo() {
        Produto produto = validProduct();
        produto.setEstoqueMinimo(-1);

        assertThrows(ValidationException.class, () -> service.cadastrar(produto));
    }

    private Produto validProduct() {
        Produto produto = new Produto();
        produto.setNome("Produto");
        produto.setPrecoCusto(new BigDecimal("1.00"));
        produto.setPrecoVenda(new BigDecimal("2.00"));
        return produto;
    }

    private static final class CapturingRepository implements ProdutoRepository {
        private Produto stored;
        @Override public Produto salvar(Produto produto) { produto.setId(1L); stored = produto; return produto; }
        @Override public void atualizar(Produto produto) { stored = produto; }
        @Override public Optional<Produto> buscarPorId(long id) { return Optional.ofNullable(stored); }
        @Override public Optional<Produto> buscarPorCodigoBarras(String codigo) { return Optional.ofNullable(stored); }
        @Override public List<Produto> listarAtivos() { return stored == null ? List.of() : List.of(stored); }
        @Override public List<Produto> pesquisar(String termo) { return listarAtivos(); }
        @Override public void desativar(long id) { stored.setAtivo(false); }
        @Override public void reativar(long id) { stored.setAtivo(true); }
    }
}
