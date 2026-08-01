package br.com.loja.pdv.domain.model;

import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.util.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Agregado em memoria que calcula itens, subtotal, desconto e total da venda. */
public final class CarrinhoVenda {
    private final Map<Long, ItemCarrinho> itens = new LinkedHashMap<>();
    private BigDecimal desconto = BigDecimal.ZERO.setScale(2);

    /** Inclui um produto novo ou soma a quantidade ao item que ja existe. */
    public ItemCarrinho adicionar(Produto produto, int quantidade) {
        validateProduct(produto);
        validateQuantity(quantidade);
        ItemCarrinho current = itens.get(produto.getId());
        int totalQuantity = quantidade + (current == null ? 0 : current.getQuantidade());
        validateStock(produto, totalQuantity);
        if (current == null) {
            current = new ItemCarrinho(produto, quantidade);
            itens.put(produto.getId(), current);
        } else {
            current.setQuantidade(totalQuantity);
        }
        ensureDiscountFits();
        return current;
    }

    /** Substitui a quantidade de um item depois de validar estoque e limites. */
    public void alterarQuantidade(long produtoId, int quantidade) {
        validateQuantity(quantidade);
        ItemCarrinho item = requireItem(produtoId);
        validateStock(item.getProduto(), quantidade);
        item.setQuantidade(quantidade);
        ensureDiscountFits();
    }

    /** Retira um produto do carrinho usando seu identificador persistido. */
    public void remover(long produtoId) {
        if (itens.remove(produtoId) == null) {
            throw new ValidationException("Item não encontrado no carrinho.");
        }
        if (desconto.compareTo(getSubtotal()) > 0) desconto = getSubtotal();
    }

    /** Remove itens e desconto para iniciar uma nova venda. */
    public void limpar() {
        itens.clear();
        desconto = BigDecimal.ZERO.setScale(2);
    }

    /** Define o desconto total sem permitir valor negativo ou maior que o subtotal. */
    public void aplicarDesconto(BigDecimal valor) {
        BigDecimal normalized = validateMoney(valor);
        if (normalized.compareTo(getSubtotal()) > 0) {
            throw new ValidationException("O desconto não pode superar o subtotal.");
        }
        desconto = normalized;
    }

    /** Retorna uma copia imutavel para impedir alteracoes externas no mapa interno. */
    public List<ItemCarrinho> getItens() {
        return List.copyOf(new ArrayList<>(itens.values()));
    }

    /** Informa se ainda nao existe nenhum item na venda atual. */
    public boolean isVazio() {
        return itens.isEmpty();
    }

    /** Soma os subtotais de todos os itens usando BigDecimal. */
    public BigDecimal getSubtotal() {
        return itens.values().stream()
                .map(ItemCarrinho::getSubtotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    /** Retorna o desconto atualmente aplicado. */
    public BigDecimal getDesconto() {
        return desconto;
    }

    /** Calcula o total liquido sem permitir resultado negativo. */
    public BigDecimal getTotal() {
        return getSubtotal().subtract(desconto);
    }

    /** Localiza o item obrigatorio ou informa que ele nao esta no carrinho. */
    private ItemCarrinho requireItem(long produtoId) {
        ItemCarrinho item = itens.get(produtoId);
        if (item == null) throw new ValidationException("Item não encontrado no carrinho.");
        return item;
    }

    /** Garante que apenas produtos persistidos e ativos entrem na venda. */
    private void validateProduct(Produto produto) {
        if (produto == null || produto.getId() == null || produto.getId() <= 0) {
            throw new ValidationException("Produto não encontrado.");
        }
        if (!produto.isAtivo()) throw new ValidationException("O produto está inativo.");
        if (produto.getPrecoVenda() == null) {
            throw new ValidationException("O produto não possui preço de venda.");
        }
    }

    /** Rejeita quantidades nulas, zero ou negativas. */
    private void validateQuantity(int quantidade) {
        if (quantidade <= 0) {
            throw new ValidationException("A quantidade deve ser maior que zero.");
        }
    }

    /** Impede que a quantidade vendida ultrapasse o saldo atual. */
    private void validateStock(Produto produto, int quantidade) {
        if (quantidade > produto.getQuantidadeEstoque()) {
            throw new ValidationException("Estoque insuficiente para " + produto.getNome() + ".");
        }
    }

    /** Normaliza valores monetarios para duas casas sem arredondamento silencioso. */
    private BigDecimal validateMoney(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new ValidationException("O desconto não pode ser negativo.");
        }
        try {
            MoneyUtils.toCents(value);
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValidationException("O desconto deve possuir no máximo duas casas decimais.");
        }
    }

    /** Impede que o desconto fique maior que o subtotal depois de qualquer alteracao. */
    private void ensureDiscountFits() {
        if (desconto.compareTo(getSubtotal()) > 0) {
            desconto = getSubtotal();
        }
    }
}
