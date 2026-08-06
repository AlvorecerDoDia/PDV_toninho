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

/** Testa entrada e ajuste direto do saldo. */
class EstoqueServiceTest {
    private MemoryStockRepository estoque;
    private EstoqueService service;

    @BeforeEach
    void setUp() {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setAtivo(true);
        estoque = new MemoryStockRepository();
        service = new EstoqueService(estoque, new SingleProductRepository(produto));
    }

    @Test
    void deveRegistrarEntrada() {
        MovimentacaoEstoque movimento = service.registrarEntrada(1, 5, "Compra");
        assertEquals(TipoMovimentacaoEstoque.ENTRADA, movimento.getTipo());
        assertEquals(5, movimento.getQuantidadePosterior());
    }

    @Test
    void deveAjustarSaldoParaCimaEParaBaixo() {
        service.registrarEntrada(1, 10, null);
        assertEquals(7, service.ajustarSaldo(1, 7, "Contagem").getQuantidadePosterior());
        assertEquals(12, service.ajustarSaldo(1, 12, "Reposicao").getQuantidadePosterior());
    }

    @Test
    void deveExigirMotivoNoAjuste() {
        assertThrows(ValidationException.class, () -> service.ajustarSaldo(1, 2, " "));
    }

    private static final class MemoryStockRepository implements EstoqueRepository {
        private int saldo;
        @Override public MovimentacaoEstoque registrar(MovimentacaoEstoque movimento) {
            movimento.setId(1L);
            movimento.setQuantidadeAnterior(saldo);
            saldo = movimento.getTipo().apply(saldo, movimento.getQuantidade());
            movimento.setQuantidadePosterior(saldo);
            return movimento;
        }
        @Override public int buscarSaldo(long produtoId) { return saldo; }
        @Override public List<MovimentacaoEstoque> listar(
                long produtoId, LocalDateTime inicio, LocalDateTime fim) { return List.of(); }
    }

    private record SingleProductRepository(Produto produto) implements ProdutoRepository {
        @Override public Produto salvar(Produto value) { return value; }
        @Override public void atualizar(Produto value) { }
        @Override public Optional<Produto> buscarPorId(long id) { return Optional.of(produto); }
        @Override public Optional<Produto> buscarPorCodigoBarras(String codigo) { return Optional.empty(); }
        @Override public List<Produto> listarAtivos() { return List.of(produto); }
        @Override public List<Produto> pesquisar(String termo) { return List.of(produto); }
        @Override public void desativar(long id) { produto.setAtivo(false); }
        @Override public void reativar(long id) { produto.setAtivo(true); }
    }
}
