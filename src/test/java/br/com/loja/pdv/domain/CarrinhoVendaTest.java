package br.com.loja.pdv.domain;

import br.com.loja.pdv.domain.model.CarrinhoVenda;
import br.com.loja.pdv.domain.model.ItemCarrinho;
import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CarrinhoVendaTest {

    @Test
    void deveAdicionarProduto() {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        carrinho.adicionar(product(1, "Arroz", "12.50", 10, true), 2);
        assertEquals(1, carrinho.getItens().size());
        assertEquals(2, carrinho.getItens().getFirst().getQuantidade());
        assertEquals(new BigDecimal("25.00"), carrinho.getTotal());
    }

    @Test
    void deveAcumularProdutoRepetido() {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        Produto produto = product(1, "Arroz", "10.00", 10, true);
        carrinho.adicionar(produto, 2);
        carrinho.adicionar(produto, 3);
        assertEquals(1, carrinho.getItens().size());
        assertEquals(5, carrinho.getItens().getFirst().getQuantidade());
    }

    @Test
    void deveAlterarQuantidade() {
        CarrinhoVenda carrinho = cartWithOneItem();
        carrinho.alterarQuantidade(1, 4);
        assertEquals(4, carrinho.getItens().getFirst().getQuantidade());
    }

    @Test
    void deveRemoverItemELimparCarrinho() {
        CarrinhoVenda carrinho = cartWithOneItem();
        carrinho.aplicarDesconto(BigDecimal.ONE);
        carrinho.remover(1);
        assertTrue(carrinho.isVazio());
        assertEquals(new BigDecimal("0.00"), carrinho.getDesconto());
        carrinho.adicionar(product(2, "Feijão", "8.00", 5, true), 1);
        carrinho.limpar();
        assertTrue(carrinho.isVazio());
    }

    @Test
    void deveCalcularSubtotalDescontoETotal() {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        carrinho.adicionar(product(1, "Arroz", "10.00", 10, true), 2);
        carrinho.adicionar(product(2, "Feijão", "7.50", 10, true), 2);
        carrinho.aplicarDesconto(new BigDecimal("5.00"));
        assertEquals(new BigDecimal("35.00"), carrinho.getSubtotal());
        assertEquals(new BigDecimal("5.00"), carrinho.getDesconto());
        assertEquals(new BigDecimal("30.00"), carrinho.getTotal());
    }

    @Test
    void deveImpedirDescontoMaiorQueSubtotalOuInvalido() {
        CarrinhoVenda carrinho = cartWithOneItem();
        assertThrows(ValidationException.class, () ->
                carrinho.aplicarDesconto(new BigDecimal("11.00")));
        assertThrows(ValidationException.class, () ->
                carrinho.aplicarDesconto(new BigDecimal("-1.00")));
        assertThrows(ValidationException.class, () ->
                carrinho.aplicarDesconto(new BigDecimal("1.001")));
    }

    @Test
    void deveImpedirProdutoInexistenteOuInativo() {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        assertThrows(ValidationException.class, () -> carrinho.adicionar(null, 1));
        assertThrows(ValidationException.class, () ->
                carrinho.adicionar(product(1, "Inativo", "10.00", 5, false), 1));
    }

    @Test
    void deveImpedirQuantidadeInvalida() {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        Produto produto = product(1, "Arroz", "10.00", 5, true);
        assertThrows(ValidationException.class, () -> carrinho.adicionar(produto, 0));
        carrinho.adicionar(produto, 1);
        assertThrows(ValidationException.class, () -> carrinho.alterarQuantidade(1, -1));
    }

    @Test
    void deveImpedirEstoqueInsuficienteAoAdicionarOuAcumular() {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        Produto produto = product(1, "Arroz", "10.00", 3, true);
        assertThrows(ValidationException.class, () -> carrinho.adicionar(produto, 4));
        carrinho.adicionar(produto, 2);
        assertThrows(ValidationException.class, () -> carrinho.adicionar(produto, 2));
        assertThrows(ValidationException.class, () -> carrinho.alterarQuantidade(1, 4));
    }

    @Test
    void deveIniciarVazio() {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        assertTrue(carrinho.isVazio());
        assertEquals(new BigDecimal("0.00"), carrinho.getSubtotal());
        assertEquals(new BigDecimal("0.00"), carrinho.getTotal());
        assertEquals(java.util.List.<ItemCarrinho>of(), carrinho.getItens());
    }

    private CarrinhoVenda cartWithOneItem() {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        carrinho.adicionar(product(1, "Arroz", "10.00", 10, true), 1);
        return carrinho;
    }

    private Produto product(
            long id, String name, String price, int stock, boolean active) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(name);
        produto.setPrecoVenda(new BigDecimal(price));
        produto.setQuantidadeEstoque(stock);
        produto.setAtivo(active);
        return produto;
    }
}
