package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.FormaPagamento;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.util.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

/** Cria um unico pagamento para cada venda. */
public final class PagamentoService {
    private final Clock clock;

    public PagamentoService() {
        this(Clock.systemDefaultZone());
    }

    PagamentoService(Clock clock) {
        this.clock = clock;
    }

    public Pagamento criar(
            FormaPagamento forma, BigDecimal valorInformado, BigDecimal total) {
        if (forma == null) {
            throw new ValidationException("Informe a forma de pagamento.");
        }
        BigDecimal totalNormalizado = normalizarPositivo(total, "total da venda");
        BigDecimal valor;
        if (forma == FormaPagamento.DINHEIRO) {
            valor = normalizarPositivo(valorInformado, "valor recebido");
            if (valor.compareTo(totalNormalizado) < 0) {
                throw new ValidationException("O valor recebido é insuficiente.");
            }
        } else {
            valor = totalNormalizado;
        }
        Pagamento pagamento = new Pagamento();
        pagamento.setForma(forma);
        pagamento.setValor(valor);
        pagamento.setCriadoEm(LocalDateTime.now(clock));
        return pagamento;
    }

    public BigDecimal calcularTroco(BigDecimal total, Pagamento pagamento) {
        if (pagamento == null || pagamento.getForma() == null) {
            throw new ValidationException("Pagamento inválido.");
        }
        if (pagamento.getForma() != FormaPagamento.DINHEIRO) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal troco = pagamento.getValor().subtract(total);
        if (troco.signum() < 0) {
            throw new ValidationException("O valor recebido é insuficiente.");
        }
        return troco.setScale(2);
    }

    private BigDecimal normalizarPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) {
            throw new ValidationException("O " + campo + " deve ser maior que zero.");
        }
        try {
            MoneyUtils.toCents(valor);
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValidationException(
                    "O " + campo + " deve possuir no máximo duas casas decimais.");
        }
    }
}
