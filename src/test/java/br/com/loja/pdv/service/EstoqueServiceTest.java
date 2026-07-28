package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.TipoMovimentacaoEstoque;
import br.com.loja.pdv.domain.model.MovimentacaoEstoque;
import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.EstoqueRepository;
import br.com.loja.pdv.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstoqueServiceTest {
    private Produto produto;
    private EstoqueService service;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setId(1L);
        produto.setAtivo(true);
        service = new EstoqueService(new MemoryStockRepository(), new SingleProductRepository(produto));
    }

    @Test
    void deveExigirQuantidadePositiva() {
        assertThrows(ValidationException.class, () ->
                service.registrar(1, TipoMovimentacaoEstoque.ENTRADA, 0, null));
    }

    @Test
    void deveExigirMotivoENormalizaLo() {
        assertThrows(ValidationException.class, () ->
                service.registrar(1, TipoMovimentacaoEstoque.PERDA, 1, " "));

        MovimentacaoEstoque movement = service.registrar(
                1, TipoMovimentacaoEstoque.AJUSTE_POSITIVO, 1, "  contagem   física ");
        assertEquals("contagem física", movement.getMotivo());
    }

    @Test
    void deveImpedirSaidaDeVendaManual() {
        assertThrows(ValidationException.class, () ->
                service.registrar(1, TipoMovimentacaoEstoque.SAIDA_VENDA, 1, null));
    }

    @Test
    void deveImpedirProdutoInativo() {
        produto.setAtivo(false);
        assertThrows(ValidationException.class, () ->
                service.registrar(1, TipoMovimentacaoEstoque.ENTRADA, 1, null));
    }

    private static final class MemoryStockRepository implements EstoqueRepository {
        private int balance;
        private MovimentacaoEstoque last;
        @Override public MovimentacaoEstoque registrar(MovimentacaoEstoque movement) {
            movement.setId(1L);
            movement.setQuantidadeAnterior(balance);
            balance = movement.getTipo().apply(balance, movement.getQuantidade());
            movement.setQuantidadePosterior(balance);
            last = movement;
            return movement;
        }
        @Override public int buscarSaldo(long produtoId) { return balance; }
        @Override public List<MovimentacaoEstoque> listar(
                long produtoId, LocalDateTime inicio, LocalDateTime fim) {
            return last == null ? List.of() : List.of(last);
        }
    }

    private static final class SingleProductRepository implements ProdutoRepository {
        private Produto produto;
        private SingleProductRepository(Produto produto) { this.produto = produto; }
        @Override public Produto salvar(Produto value) { produto = value; return value; }
        @Override public void atualizar(Produto value) { produto = value; }
        @Override public Optional<Produto> buscarPorId(long id) { return Optional.ofNullable(produto); }
        @Override public Optional<Produto> buscarPorCodigoBarras(String codigo) { return Optional.empty(); }
        @Override public List<Produto> listarAtivos() {
            return produto != null && produto.isAtivo() ? List.of(produto) : List.of();
        }
        @Override public List<Produto> pesquisar(String termo) { return listarAtivos(); }
        @Override public void desativar(long id) { produto.setAtivo(false); }
        @Override public void reativar(long id) { produto.setAtivo(true); }
    }
}
