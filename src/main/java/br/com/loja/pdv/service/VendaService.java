package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.Permissao;
import br.com.loja.pdv.domain.enums.StatusVenda;
import br.com.loja.pdv.domain.model.*;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.CaixaRepository;
import br.com.loja.pdv.repository.ProdutoRepository;
import br.com.loja.pdv.repository.VendaRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public final class VendaService {
    private static final DateTimeFormatter NUMBER_DATE =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VendaRepository vendas;
    private final ProdutoRepository produtos;
    private final CaixaRepository caixas;
    private final SessaoUsuario sessao;
    private final PagamentoService pagamentos;
    private final Clock clock;

    public VendaService(
            VendaRepository vendas, ProdutoRepository produtos, CaixaRepository caixas,
            SessaoUsuario sessao, PagamentoService pagamentos) {
        this(vendas, produtos, caixas, sessao, pagamentos, Clock.systemDefaultZone());
    }

    VendaService(
            VendaRepository vendas, ProdutoRepository produtos, CaixaRepository caixas,
            SessaoUsuario sessao, PagamentoService pagamentos, Clock clock) {
        this.vendas = vendas;
        this.produtos = produtos;
        this.caixas = caixas;
        this.sessao = sessao;
        this.pagamentos = pagamentos;
        this.clock = clock;
    }

    public Venda finalizar(CarrinhoVenda carrinho, List<Pagamento> formasPagamento) {
        sessao.exigir(Permissao.VENDAS);
        Usuario operador = sessao.atual().orElseThrow();
        if (carrinho == null || carrinho.isVazio()) {
            throw new ValidationException("Não é possível finalizar uma venda sem itens.");
        }
        if (carrinho.getDesconto().signum() > 0) {
            sessao.exigir(Permissao.DESCONTOS);
        }
        Caixa caixa = caixas.buscarAbertoPorUsuario(operador.getId())
                .orElseThrow(() -> new ValidationException(
                        "Abra o caixa antes de finalizar a venda."));
        List<Pagamento> safePayments =
                formasPagamento == null ? List.of() : List.copyOf(formasPagamento);
        var change = pagamentos.validarECalcularTroco(carrinho.getTotal(), safePayments);
        LocalDateTime now = LocalDateTime.now(clock);

        Venda venda = new Venda();
        venda.setNumero(createNumber(now));
        venda.setOperadorId(operador.getId());
        venda.setCaixaId(caixa.getId());
        venda.setStatus(StatusVenda.FINALIZADA);
        venda.setSubtotal(carrinho.getSubtotal());
        venda.setDesconto(carrinho.getDesconto());
        venda.setTotal(carrinho.getTotal());
        venda.setTroco(change);
        venda.setCriadoEm(now);
        for (ItemCarrinho cartItem : carrinho.getItens()) {
            venda.getItens().add(toSaleItem(cartItem));
        }
        venda.getPagamentos().addAll(safePayments);

        Venda finalized = vendas.finalizar(venda);
        carrinho.limpar();
        return finalized;
    }

    private ItemVenda toSaleItem(ItemCarrinho cartItem) {
        Produto current = produtos.buscarPorId(cartItem.getProduto().getId())
                .orElseThrow(() -> new ValidationException("Produto não encontrado."));
        if (!current.isAtivo()) {
            throw new ValidationException("O produto está inativo: " + current.getNome() + ".");
        }
        if (current.getQuantidadeEstoque() < cartItem.getQuantidade()) {
            throw new ValidationException("Estoque insuficiente para " + current.getNome() + ".");
        }
        if (current.getPrecoVenda().compareTo(cartItem.getPrecoUnitario()) != 0) {
            throw new ValidationException(
                    "O preço de " + current.getNome() + " foi alterado. Atualize o carrinho.");
        }
        ItemVenda item = new ItemVenda();
        item.setProdutoId(current.getId());
        item.setProdutoNome(current.getNome());
        item.setQuantidade(cartItem.getQuantidade());
        item.setCustoUnitario(current.getPrecoCusto());
        item.setPrecoUnitario(cartItem.getPrecoUnitario());
        item.setSubtotal(cartItem.getSubtotal());
        return item;
    }

    private String createNumber(LocalDateTime timestamp) {
        return "V" + timestamp.format(NUMBER_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
