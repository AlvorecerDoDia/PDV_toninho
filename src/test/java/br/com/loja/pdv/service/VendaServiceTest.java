package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.*;
import br.com.loja.pdv.domain.model.*;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.CaixaRepository;
import br.com.loja.pdv.repository.ProdutoRepository;
import br.com.loja.pdv.repository.VendaRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VendaServiceTest {

    @Test
    void deveImpedirVendaVazia() {
        Fixture fixture = new Fixture(PerfilUsuario.GERENTE);
        assertThrows(ValidationException.class, () ->
                fixture.service.finalizar(new CarrinhoVenda(), List.of()));
    }

    @Test
    void deveBloquearDescontoParaOperador() {
        Fixture fixture = new Fixture(PerfilUsuario.OPERADOR);
        CarrinhoVenda cart = fixture.cart();
        cart.aplicarDesconto(BigDecimal.ONE);
        assertThrows(ValidationException.class, () ->
                fixture.service.finalizar(cart, List.of(
                        fixture.payments.criar(
                                FormaPagamento.DINHEIRO, new BigDecimal("9.00")))));
        assertFalse(cart.isVazio());
    }

    @Test
    void deveMontarVendaComDadosHistoricosELimparCarrinhoAposSucesso() {
        Fixture fixture = new Fixture(PerfilUsuario.GERENTE);
        CarrinhoVenda cart = fixture.cart();
        Venda sale = fixture.service.finalizar(cart, List.of(
                fixture.payments.criar(
                        FormaPagamento.DINHEIRO, new BigDecimal("10.00"))));
        assertEquals(99L, sale.getId());
        assertEquals(StatusVenda.FINALIZADA, sale.getStatus());
        assertEquals(new BigDecimal("4.00"), sale.getItens().getFirst().getCustoUnitario());
        assertTrue(cart.isVazio());
    }

    private static final class Fixture {
        private final Produto product = product();
        private final PagamentoService payments = new PagamentoService();
        private final VendaService service;

        private Fixture(PerfilUsuario profile) {
            Usuario user = new Usuario();
            user.setId(1L);
            user.setNome("Usuário");
            user.setAtivo(true);
            user.setPerfil(profile);
            SessaoUsuario session = new SessaoUsuario();
            session.iniciar(user);

            Caixa cash = new Caixa();
            cash.setId(2L);
            cash.setUsuarioId(user.getId());
            cash.setStatus(StatusCaixa.ABERTO);
            cash.setValorAbertura(BigDecimal.ZERO.setScale(2));
            cash.setAbertoEm(LocalDateTime.now());
            service = new VendaService(
                    new FakeSaleRepository(),
                    new FakeProductRepository(product),
                    new FakeCashRepository(cash),
                    session,
                    payments);
        }

        private CarrinhoVenda cart() {
            CarrinhoVenda cart = new CarrinhoVenda();
            cart.adicionar(product, 1);
            return cart;
        }

        private static Produto product() {
            Produto product = new Produto();
            product.setId(3L);
            product.setNome("Produto");
            product.setPrecoCusto(new BigDecimal("4.00"));
            product.setPrecoVenda(new BigDecimal("10.00"));
            product.setQuantidadeEstoque(5);
            product.setAtivo(true);
            return product;
        }
    }

    private static final class FakeSaleRepository implements VendaRepository {
        private Venda saved;

        @Override
        public Venda finalizar(Venda venda) {
            venda.setId(99L);
            saved = venda;
            return venda;
        }

        @Override
        public Optional<Venda> buscarPorId(long id) {
            return saved != null && saved.getId() == id ? Optional.of(saved) : Optional.empty();
        }

        @Override
        public Optional<Venda> buscarPorNumero(String number) {
            return saved != null && saved.getNumero().equals(number)
                    ? Optional.of(saved) : Optional.empty();
        }

        @Override
        public List<Venda> listar(
                LocalDateTime start, LocalDateTime end, Long operatorId) {
            return saved == null ? List.of() : List.of(saved);
        }

        @Override
        public List<ItemVenda> listarItens(long vendaId) {
            return buscarPorId(vendaId)
                    .map(value -> List.copyOf(value.getItens()))
                    .orElseGet(List::of);
        }

        @Override
        public Venda cancelar(
                long saleId, long userId, String reason, LocalDateTime canceledAt) {
            Venda sale = buscarPorId(saleId).orElseThrow();
            sale.setStatus(StatusVenda.CANCELADA);
            sale.setMotivoCancelamento(reason);
            sale.setCanceladoEm(canceledAt);
            return sale;
        }
    }

    private static final class FakeProductRepository implements ProdutoRepository {
        private final Produto product;

        private FakeProductRepository(Produto product) {
            this.product = product;
        }

        @Override public Produto salvar(Produto value) { return value; }
        @Override public void atualizar(Produto value) { product.setNome(value.getNome()); }
        @Override public Optional<Produto> buscarPorId(long id) {
            return product.getId() == id ? Optional.of(product) : Optional.empty();
        }
        @Override public Optional<Produto> buscarPorCodigoBarras(String code) {
            return Optional.empty();
        }
        @Override public List<Produto> listarAtivos() { return List.of(product); }
        @Override public List<Produto> pesquisar(String term) { return List.of(product); }
        @Override public void desativar(long id) { product.setAtivo(false); }
        @Override public void reativar(long id) { product.setAtivo(true); }
    }

    private static final class FakeCashRepository implements CaixaRepository {
        private final Caixa cash;
        private final List<MovimentacaoCaixa> movements = new ArrayList<>();

        private FakeCashRepository(Caixa cash) {
            this.cash = cash;
        }

        @Override public Caixa abrir(Caixa value, MovimentacaoCaixa opening) {
            movements.add(opening);
            return value;
        }
        @Override public Optional<Caixa> buscarPorId(long id) {
            return cash.getId() == id ? Optional.of(cash) : Optional.empty();
        }
        @Override public Optional<Caixa> buscarAbertoPorUsuario(long userId) {
            return cash.getUsuarioId() == userId && cash.getStatus() == StatusCaixa.ABERTO
                    ? Optional.of(cash) : Optional.empty();
        }
        @Override public MovimentacaoCaixa registrar(MovimentacaoCaixa movement) {
            movements.add(movement);
            return movement;
        }
        @Override public Caixa fechar(long id, BigDecimal counted, LocalDateTime closedAt) {
            cash.setStatus(StatusCaixa.FECHADO);
            cash.setValorContado(counted);
            cash.setFechadoEm(closedAt);
            return cash;
        }
        @Override public BigDecimal buscarDinheiroEsperado(long id) {
            return cash.getValorAbertura();
        }
        @Override public List<MovimentacaoCaixa> listarMovimentacoes(long id) {
            return List.copyOf(movements);
        }
    }
}
