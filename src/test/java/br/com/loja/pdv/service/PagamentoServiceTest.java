package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.FormaPagamento;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Testa o pagamento unico usado pela venda simplificada. */
class PagamentoServiceTest {
    private final PagamentoService service = new PagamentoService();

    @Test
    void deveCalcularTrocoEmDinheiro() {
        Pagamento pagamento = service.criar(
                FormaPagamento.DINHEIRO,
                new BigDecimal("120.00"),
                new BigDecimal("100.00"));

        assertEquals(new BigDecimal("20.00"),
                service.calcularTroco(new BigDecimal("100.00"), pagamento));
    }

    @Test
    void deveUsarTotalExatoEmPagamentoEletronico() {
        Pagamento pagamento = service.criar(
                FormaPagamento.PIX,
                new BigDecimal("999.00"),
                new BigDecimal("75.50"));

        assertEquals(new BigDecimal("75.50"), pagamento.getValor());
        assertEquals(new BigDecimal("0.00"),
                service.calcularTroco(new BigDecimal("75.50"), pagamento));
    }

    @Test
    void deveRecusarDinheiroInsuficiente() {
        assertThrows(ValidationException.class, () -> service.criar(
                FormaPagamento.DINHEIRO,
                new BigDecimal("40.00"),
                new BigDecimal("50.00")));
    }
}
