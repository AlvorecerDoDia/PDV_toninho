package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.FormaPagamento;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Testa regras de negocio sem depender da interface grafica. */
class PagamentoServiceTest {
    private final PagamentoService service = new PagamentoService();

    /** Verifica o cenario: deve calcular troco de pagamento em dinheiro. */
    @Test
    void deveCalcularTrocoDePagamentoEmDinheiro() {
        Pagamento cash = service.criar(FormaPagamento.DINHEIRO, new BigDecimal("120.00"));
        assertEquals(new BigDecimal("20.00"),
                service.validarECalcularTroco(new BigDecimal("100.00"), List.of(cash)));
    }

    /** Verifica o cenario: deve aceitar pix ecartoes sem troco. */
    @Test
    void deveAceitarPixECartoesSemTroco() {
        for (FormaPagamento type : List.of(
                FormaPagamento.PIX, FormaPagamento.CARTAO_DEBITO,
                FormaPagamento.CARTAO_CREDITO)) {
            Pagamento payment = service.criar(type, new BigDecimal("100.00"));
            assertEquals(new BigDecimal("0.00"),
                    service.validarECalcularTroco(
                            new BigDecimal("100.00"), List.of(payment)));
        }
    }

    /** Verifica o cenario: deve aceitar pagamento combinado. */
    @Test
    void deveAceitarPagamentoCombinado() {
        List<Pagamento> payments = List.of(
                service.criar(FormaPagamento.DINHEIRO, new BigDecimal("40.00")),
                service.criar(FormaPagamento.PIX, new BigDecimal("60.00")));
        assertEquals(new BigDecimal("0.00"),
                service.validarECalcularTroco(new BigDecimal("100.00"), payments));
    }

    /** Verifica o cenario: deve impedir pagamento insuficiente. */
    @Test
    void deveImpedirPagamentoInsuficiente() {
        Pagamento payment = service.criar(FormaPagamento.PIX, new BigDecimal("99.99"));
        assertThrows(ValidationException.class, () ->
                service.validarECalcularTroco(
                        new BigDecimal("100.00"), List.of(payment)));
    }

    /** Verifica o cenario: deve impedir troco originado de pix ou cartao. */
    @Test
    void deveImpedirTrocoOriginadoDePixOuCartao() {
        Pagamento payment = service.criar(FormaPagamento.PIX, new BigDecimal("101.00"));
        assertThrows(ValidationException.class, () ->
                service.validarECalcularTroco(
                        new BigDecimal("100.00"), List.of(payment)));
    }

    /** Verifica o cenario: deve impedir valores invalidos. */
    @Test
    void deveImpedirValoresInvalidos() {
        assertThrows(ValidationException.class, () ->
                service.criar(FormaPagamento.DINHEIRO, BigDecimal.ZERO));
        assertThrows(ValidationException.class, () ->
                service.criar(FormaPagamento.DINHEIRO, new BigDecimal("-1.00")));
        assertThrows(ValidationException.class, () ->
                service.criar(FormaPagamento.DINHEIRO, new BigDecimal("1.001")));
    }
}
