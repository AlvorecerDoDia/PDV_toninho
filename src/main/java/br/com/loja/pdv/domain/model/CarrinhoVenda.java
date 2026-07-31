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

    public void alterarQuantidade(long produtoId, int quantidade) {
        validateQuantity(quantidade);
        ItemCarrinho item = requireItem(produtoId);
        validateStock(item.getProduto(), quantidade);
        item.setQuantidade(quantidade);
        ensureDiscountFits();
    }

    public void remover(long produtoId) {
        if (itens.remove(produtoId) == null) {
            throw new ValidationException("Item não encontrado no carrinho.");
        }
        if (desconto.compareTo(getSubtotal()) > 0) desconto = getSubtotal();
    }

    public void limpar() {
        itens.clear();
        desconto = BigDecimal.ZERO.setScale(2);
    }

    public void aplicarDesconto(BigDecimal valor) {
        BigDecimal normalized = validateMoney(valor);
        if (normalized.compareTo(getSubtotal()) > 0) {
            throw new ValidationException("O desconto não pode superar o subtotal.");
        }
        desconto = normalized;
    }

    public List<ItemCarrinho> getItens() {
        return List.copyOf(new ArrayList<>(itens.values()));
    }

    public boolean isVazio() {
        return itens.isEmpty();
    }

    public BigDecimal getSubtotal() {
        return itens.values().stream()
                .map(ItemCarrinho::getSubtotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getTotal() {
        return getSubtotal().subtract(desconto);
    }

    private ItemCarrinho requireItem(long produtoId) {
        ItemCarrinho item = itens.get(produtoId);
        if (item == null) throw new ValidationException("Item não encontrado no carrinho.");
        return item;
    }

    private void validateProduct(Produto produto) {
        if (produto == null || produto.getId() == null || produto.getId() <= 0) {
            throw new ValidationException("Produto não encontrado.");
        }
        if (!produto.isAtivo()) throw new ValidationException("O produto está inativo.");
        if (produto.getPrecoVenda() == null) {
            throw new ValidationException("O produto não possui preço de venda.");
        }
    }

    private void validateQuantity(int quantidade) {
        if (quantidade <= 0) {
            throw new ValidationException("A quantidade deve ser maior que zero.");
        }
    }

    private void validateStock(Produto produto, int quantidade) {
        if (quantidade > produto.getQuantidadeEstoque()) {
            throw new ValidationException("Estoque insuficiente para " + produto.getNome() + ".");
        }
    }

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

    private void ensureDiscountFits() {
        if (desconto.compareTo(getSubtotal()) > 0) {
            desconto = getSubtotal();
        }
    }
}
