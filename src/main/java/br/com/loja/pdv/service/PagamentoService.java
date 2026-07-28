package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.FormaPagamento;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.util.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Monta pagamentos, valida os valores recebidos e calcula o troco da venda.
 */
public final class PagamentoService {
    private final Clock clock;

    public PagamentoService() {
        this(Clock.systemDefaultZone());
    }

    PagamentoService(Clock clock) {
        this.clock = clock;
    }

    public Pagamento criar(FormaPagamento forma, BigDecimal valor) {
        if (forma == null) throw new ValidationException("Informe a forma de pagamento.");
        Pagamento pagamento = new Pagamento();
        pagamento.setForma(forma);
        pagamento.setValor(normalizePositive(valor));
        pagamento.setCriadoEm(LocalDateTime.now(clock));
        return pagamento;
    }

    public BigDecimal validarECalcularTroco(
            BigDecimal total, List<Pagamento> pagamentos) {
        if (total == null || total.signum() < 0) {
            throw new ValidationException("O total da venda é inválido.");
        }
        List<Pagamento> safePayments = pagamentos == null ? List.of() : pagamentos;
        BigDecimal received = BigDecimal.ZERO.setScale(2);
        BigDecimal cash = BigDecimal.ZERO.setScale(2);
        for (Pagamento payment : safePayments) {
            if (payment == null || payment.getForma() == null
                    || payment.getCriadoEm() == null) {
                throw new ValidationException("Pagamento inválido.");
            }
            BigDecimal value = normalizePositive(payment.getValor());
            received = received.add(value);
            if (payment.getForma() == FormaPagamento.DINHEIRO) cash = cash.add(value);
        }
        if (received.compareTo(total) < 0) {
            throw new ValidationException("A soma dos pagamentos é insuficiente.");
        }
        BigDecimal change = received.subtract(total);
        if (change.compareTo(cash) > 0) {
            throw new ValidationException("PIX e cartão não podem gerar troco.");
        }
        return change;
    }

    public BigDecimal totalRecebido(List<Pagamento> pagamentos) {
        if (pagamentos == null) return BigDecimal.ZERO.setScale(2);
        return pagamentos.stream()
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    private BigDecimal normalizePositive(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new ValidationException("O valor do pagamento deve ser maior que zero.");
        }
        try {
            MoneyUtils.toCents(value);
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValidationException(
                    "O pagamento deve possuir no máximo duas casas decimais.");
        }
    }
}
