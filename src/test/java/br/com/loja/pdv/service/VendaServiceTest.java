package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.FormaPagamento;
import br.com.loja.pdv.domain.enums.StatusCaixa;
import br.com.loja.pdv.domain.enums.StatusVenda;
import br.com.loja.pdv.domain.model.*;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.CaixaRepository;
import br.com.loja.pdv.repository.ProdutoRepository;
import br.com.loja.pdv.repository.VendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Garante que a simplificacao preserve venda e historico completos. */
class VendaServiceTest {
    private MemoryVendaRepository vendas;
    private VendaService service;
    private PagamentoService pagamentos;
    private Produto produto;

    @BeforeEach
    void setUp() {
        Categoria categoria = new Categoria();
        categoria.setId(10L);
        categoria.setNome("Papelaria");

        produto = new Produto();
        produto.setId(1L);
        produto.setNome("Caderno");
        produto.setCategoria(categoria);
        produto.setPrecoCusto(new BigDecimal("5.00"));
        produto.setPrecoVenda(new BigDecimal("10.00"));
        produto.setQuantidadeEstoque(20);
        produto.setAtivo(true);

        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setAtivo(true);
        SessaoUsuario sessao = new SessaoUsuario();
        sessao.iniciar(usuario);

        Caixa caixa = new Caixa();
        caixa.setId(3L);
        caixa.setUsuarioId(2L);
        caixa.setStatus(StatusCaixa.ABERTO);

        vendas = new MemoryVendaRepository();
        pagamentos = new PagamentoService();
        service = new VendaService(
                vendas,
                new SingleProductRepository(produto),
                new OpenCashRepository(caixa),
                sessao,
                pagamentos);
    }

    @Test
    void deveFinalizarComUmPagamentoEPreservarCategoriaHistorica() {
        CarrinhoVenda carrinho = new CarrinhoVenda();
        carrinho.adicionar(produto, 2);
        Pagamento pagamento = pagamentos.criar(
                FormaPagamento.DINHEIRO,
                new BigDecimal("25.00"),
                carrinho.getTotal());

        Venda venda = service.finalizar(carrinho, pagamento);

        assertEquals(StatusVenda.FINALIZADA, venda.getStatus());
        assertEquals(new BigDecimal("5.00"), venda.getTroco());
        assertEquals(1, venda.getPagamentos().size());
        assertEquals("Papelaria", venda.getItens().getFirst().getCategoriaNome());
        assertTrue(carrinho.isVazio());
    }

    @Test
    void deveManterFiltroDeVariasCategoriasNoHistorico() {
        Set<Long> categorias = Set.of(10L, 20L);
        service.listarProdutosVendidos(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 5),
                categorias);

        assertEquals(categorias, vendas.ultimoFiltroCategorias);
    }

    @Test
    void deveManterConsultaECancelamentoNoHistorico() {
        Venda venda = new Venda();
        venda.setId(8L);
        venda.setNumero("VTESTE");
        venda.setStatus(StatusVenda.FINALIZADA);
        vendas.venda = venda;

        assertEquals("VTESTE", service.buscarPorNumero(" vteste ").getNumero());
        Venda cancelada = service.cancelar(8L, "  erro   no item ");
        assertEquals(StatusVenda.CANCELADA, cancelada.getStatus());
        assertEquals("erro no item", cancelada.getMotivoCancelamento());
    }

    @Test
    void deveImpedirVendaSemItens() {
        Pagamento pagamento = pagamentos.criar(
                FormaPagamento.PIX, null, BigDecimal.ONE);
        assertThrows(ValidationException.class,
                () -> service.finalizar(new CarrinhoVenda(), pagamento));
    }

    private static final class MemoryVendaRepository implements VendaRepository {
        private Venda venda;
        private Set<Long> ultimoFiltroCategorias = Set.of();

        @Override public Venda finalizar(Venda nova) {
            nova.setId(1L);
            venda = nova;
            return nova;
        }
        @Override public Optional<Venda> buscarPorId(long id) { return Optional.ofNullable(venda); }
        @Override public Optional<Venda> buscarPorNumero(String numero) {
            return venda != null && venda.getNumero().equals(numero)
                    ? Optional.of(venda) : Optional.empty();
        }
        @Override public List<Venda> listar(
                LocalDateTime inicio, LocalDateTime fim, Long operadorId) {
            return venda == null ? List.of() : List.of(venda);
        }
        @Override public List<ItemVenda> listarItens(long vendaId) {
            return venda == null ? List.of() : new ArrayList<>(venda.getItens());
        }
        @Override public List<ProdutoVendidoHistorico> listarProdutosVendidos(
                LocalDateTime inicio, LocalDateTime fim, Set<Long> categoriaIds) {
            ultimoFiltroCategorias = Set.copyOf(categoriaIds);
            return List.of();
        }
        @Override public Venda cancelar(
                long vendaId, long usuarioId, String motivo, LocalDateTime canceladoEm) {
            venda.setStatus(StatusVenda.CANCELADA);
            venda.setMotivoCancelamento(motivo);
            venda.setCanceladoEm(canceladoEm);
            return venda;
        }
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

    private record OpenCashRepository(Caixa caixa) implements CaixaRepository {
        @Override public Caixa abrir(Caixa novo, MovimentacaoCaixa abertura) { return novo; }
        @Override public Optional<Caixa> buscarPorId(long id) { return Optional.of(caixa); }
        @Override public Optional<Caixa> buscarAbertoPorUsuario(long usuarioId) {
            return Optional.of(caixa);
        }
        @Override public Caixa fechar(
                long caixaId, BigDecimal valorContado, LocalDateTime fechadoEm) { return caixa; }
        @Override public BigDecimal buscarDinheiroEsperado(long caixaId) { return BigDecimal.ZERO; }
        @Override public List<MovimentacaoCaixa> listarMovimentacoes(long caixaId) {
            return List.of();
        }
    }
}
