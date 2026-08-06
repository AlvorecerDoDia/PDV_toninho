package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.StatusVenda;
import br.com.loja.pdv.domain.model.*;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.CaixaRepository;
import br.com.loja.pdv.repository.ProdutoRepository;
import br.com.loja.pdv.repository.VendaRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Finaliza, consulta e cancela vendas. */
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

    public Venda finalizar(CarrinhoVenda carrinho, Pagamento pagamento) {
        Usuario operador = sessao.exigirLogin();
        if (carrinho == null || carrinho.isVazio()) {
            throw new ValidationException("Não é possível finalizar uma venda sem itens.");
        }
        Caixa caixa = caixas.buscarAbertoPorUsuario(operador.getId())
                .orElseThrow(() -> new ValidationException(
                        "Abra o caixa antes de finalizar a venda."));
        var troco = pagamentos.calcularTroco(carrinho.getTotal(), pagamento);
        LocalDateTime agora = LocalDateTime.now(clock);

        Venda venda = new Venda();
        venda.setNumero(criarNumero(agora));
        venda.setOperadorId(operador.getId());
        venda.setCaixaId(caixa.getId());
        venda.setStatus(StatusVenda.FINALIZADA);
        venda.setSubtotal(carrinho.getSubtotal());
        venda.setDesconto(carrinho.getDesconto());
        venda.setTotal(carrinho.getTotal());
        venda.setTroco(troco);
        venda.setCriadoEm(agora);
        for (ItemCarrinho itemCarrinho : carrinho.getItens()) {
            venda.getItens().add(paraItemVenda(itemCarrinho));
        }
        venda.getPagamentos().add(pagamento);

        Venda finalizada = vendas.finalizar(venda);
        carrinho.limpar();
        return finalizada;
    }

    public Venda buscarPorNumero(String numero) {
        sessao.exigirLogin();
        String normalizado = numero == null ? "" : numero.strip().toUpperCase();
        if (normalizado.isEmpty()) {
            throw new ValidationException("Informe o número da venda.");
        }
        Venda venda = vendas.buscarPorNumero(normalizado)
                .orElseThrow(() -> new ValidationException("Venda não encontrada."));
        venda.getItens().clear();
        venda.getItens().addAll(vendas.listarItens(venda.getId()));
        return venda;
    }

    public List<Venda> listar(LocalDate inicio, LocalDate fim, Long operadorId) {
        sessao.exigirLogin();
        validarPeriodo(inicio, fim);
        if (operadorId != null && operadorId <= 0) {
            throw new ValidationException("Operador inválido.");
        }
        return vendas.listar(
                inicio.atStartOfDay(),
                fim.plusDays(1).atStartOfDay().minusNanos(1),
                operadorId);
    }

    public List<ProdutoVendidoHistorico> listarProdutosVendidos(
            LocalDate inicio, LocalDate fim, Set<Long> categoriaIds) {
        sessao.exigirLogin();
        validarPeriodo(inicio, fim);
        if (categoriaIds != null
                && categoriaIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ValidationException("Categoria inválida.");
        }
        Set<Long> categoriasSeguras = categoriaIds == null
                ? Set.of()
                : Set.copyOf(categoriaIds);
        return vendas.listarProdutosVendidos(
                inicio.atStartOfDay(),
                fim.plusDays(1).atStartOfDay().minusNanos(1),
                categoriasSeguras);
    }

    public Venda detalhar(long vendaId) {
        sessao.exigirLogin();
        Venda venda = vendas.buscarPorId(vendaId)
                .orElseThrow(() -> new ValidationException("Venda não encontrada."));
        venda.getItens().addAll(vendas.listarItens(vendaId));
        return venda;
    }

    public Venda cancelar(long vendaId, String motivo) {
        Usuario usuario = sessao.exigirLogin();
        String normalizado = motivo == null
                ? ""
                : motivo.strip().replaceAll("\\s+", " ");
        if (normalizado.isEmpty()) {
            throw new ValidationException("Informe o motivo do cancelamento.");
        }
        return vendas.cancelar(
                vendaId, usuario.getId(), normalizado, LocalDateTime.now(clock));
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new ValidationException("Informe um período válido.");
        }
    }

    private ItemVenda paraItemVenda(ItemCarrinho itemCarrinho) {
        Produto atual = produtos.buscarPorId(itemCarrinho.getProduto().getId())
                .orElseThrow(() -> new ValidationException("Produto não encontrado."));
        if (!atual.isAtivo()) {
            throw new ValidationException("O produto está inativo: " + atual.getNome() + ".");
        }
        if (atual.getQuantidadeEstoque() < itemCarrinho.getQuantidade()) {
            throw new ValidationException("Estoque insuficiente para " + atual.getNome() + ".");
        }
        if (atual.getPrecoVenda().compareTo(itemCarrinho.getPrecoUnitario()) != 0) {
            throw new ValidationException(
                    "O preço de " + atual.getNome() + " foi alterado. Atualize o carrinho.");
        }
        ItemVenda item = new ItemVenda();
        item.setProdutoId(atual.getId());
        item.setProdutoNome(atual.getNome());
        if (atual.getCategoria() != null) {
            item.setCategoriaId(atual.getCategoria().getId());
            item.setCategoriaNome(atual.getCategoria().getNome());
        } else {
            item.setCategoriaNome("Sem categoria");
        }
        item.setQuantidade(itemCarrinho.getQuantidade());
        item.setCustoUnitario(atual.getPrecoCusto());
        item.setPrecoUnitario(itemCarrinho.getPrecoUnitario());
        item.setSubtotal(itemCarrinho.getSubtotal());
        return item;
    }

    private String criarNumero(LocalDateTime dataHora) {
        return "V" + dataHora.format(NUMBER_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
