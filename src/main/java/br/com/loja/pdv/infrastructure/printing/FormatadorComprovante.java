package br.com.loja.pdv.infrastructure.printing;

import br.com.loja.pdv.domain.model.ItemVenda;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.domain.model.Venda;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Constroi o texto do comprovante usando apenas valores historicos da venda. */
public final class FormatadorComprovante {
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String SEPARATOR = "----------------------------------------";
    private final String storeName;

    /** Define o nome da loja exibido no cabecalho do comprovante. */
    public FormatadorComprovante(String storeName) {
        String normalized = storeName == null ? "" : storeName.strip();
        this.storeName = normalized.isEmpty() ? "PDV Toninho" : normalized;
    }

    /** Monta todo o comprovante nao fiscal a partir dos dados historicos da venda. */
    public String formatar(Venda venda, boolean segundaVia) {
        if (venda == null) throw new IllegalArgumentException("A venda é obrigatória.");
        StringBuilder receipt = new StringBuilder();
        receipt.append(center(storeName)).append('\n');
        if (segundaVia) receipt.append(center("SEGUNDA VIA")).append('\n');
        receipt.append(center("COMPROVANTE NÃO FISCAL")).append('\n');
        receipt.append(SEPARATOR).append('\n');
        receipt.append("Venda: ").append(venda.getNumero()).append('\n');
        receipt.append("Data: ").append(venda.getCriadoEm().format(DATE_TIME)).append('\n');
        receipt.append("Operador: ").append(venda.getOperadorId()).append('\n');
        receipt.append("Status: ").append(venda.getStatus()).append('\n');
        if (venda.getMotivoCancelamento() != null) {
            receipt.append("Cancelamento: ")
                    .append(venda.getMotivoCancelamento()).append('\n');
        }
        receipt.append(SEPARATOR).append('\n');
        for (ItemVenda item : venda.getItens()) appendItem(receipt, item);
        receipt.append(SEPARATOR).append('\n');
        receipt.append(line("Subtotal", venda.getSubtotal()));
        receipt.append(line("Desconto", venda.getDesconto()));
        receipt.append(line("TOTAL", venda.getTotal()));
        receipt.append(SEPARATOR).append('\n');
        for (Pagamento payment : venda.getPagamentos()) appendPayment(receipt, payment);
        BigDecimal received = venda.getPagamentos().stream()
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        receipt.append(line("Valor recebido", received));
        receipt.append(line("Troco", venda.getTroco()));
        receipt.append(SEPARATOR).append('\n');
        receipt.append(center("Obrigado pela preferência!")).append('\n');
        return receipt.toString();
    }

    /** Acrescenta quantidade, nome, preco unitario e subtotal de um item. */
    private void appendItem(StringBuilder receipt, ItemVenda item) {
        receipt.append(item.getProdutoNome()).append('\n');
        receipt.append(item.getQuantidade()).append(" x ")
                .append(CURRENCY.format(item.getPrecoUnitario()))
                .append(" = ").append(CURRENCY.format(item.getSubtotal())).append('\n');
    }

    /** Acrescenta uma forma de pagamento e seu valor. */
    private void appendPayment(StringBuilder receipt, Pagamento payment) {
        receipt.append(line(payment.getForma().name(), payment.getValor()));
    }

    /** Cria uma linha de largura fixa para separar secoes do comprovante. */
    private String line(String label, BigDecimal value) {
        return "%-22s %17s%n".formatted(label, CURRENCY.format(value));
    }

    /** Centraliza um texto curto respeitando a largura da bobina. */
    private String center(String text) {
        int width = 40;
        if (text.length() >= width) return text;
        return " ".repeat((width - text.length()) / 2) + text;
    }
}
