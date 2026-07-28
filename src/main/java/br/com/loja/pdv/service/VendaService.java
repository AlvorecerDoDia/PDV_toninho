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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra a validação, finalização e o cancelamento das vendas do PDV.
 */
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

        // O repositório confirma novamente caixa e estoque dentro da transação,
        // pois eles podem ter mudado desde que os itens entraram no carrinho.
        Venda finalized = vendas.finalizar(venda);
        carrinho.limpar();
        return finalized;
    }

    public Venda buscarPorNumero(String numero) {
        sessao.exigir(Permissao.RELATORIOS);
        String normalized = numero == null ? "" : numero.strip().toUpperCase();
        if (normalized.isEmpty()) {
            throw new ValidationException("Informe o número da venda.");
        }
        Venda venda = vendas.buscarPorNumero(normalized)
                .orElseThrow(() -> new ValidationException("Venda não encontrada."));
        venda.getItens().clear();
        venda.getItens().addAll(vendas.listarItens(venda.getId()));
        return venda;
    }

    public List<Venda> listar(LocalDate inicio, LocalDate fim, Long operadorId) {
        sessao.exigir(Permissao.RELATORIOS);
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new ValidationException("Informe um período válido.");
        }
        if (operadorId != null && operadorId <= 0) {
            throw new ValidationException("Operador inválido.");
        }
        return vendas.listar(
                inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay().minusNanos(1),
                operadorId);
    }

    public Venda detalhar(long vendaId) {
        sessao.exigir(Permissao.RELATORIOS);
        Venda venda = vendas.buscarPorId(vendaId)
                .orElseThrow(() -> new ValidationException("Venda não encontrada."));
        venda.getItens().addAll(vendas.listarItens(vendaId));
        return venda;
    }

    public Venda cancelar(long vendaId, String motivo) {
        sessao.exigir(Permissao.CANCELAMENTOS);
        Usuario usuario = sessao.atual().orElseThrow();
        String normalized = motivo == null ? "" : motivo.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw new ValidationException("Informe o motivo do cancelamento.");
        }
        return vendas.cancelar(
                vendaId, usuario.getId(), normalized, LocalDateTime.now(clock));
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
