package br.com.loja.pdv.infrastructure.printing;

import br.com.loja.pdv.domain.enums.FormaPagamento;
import br.com.loja.pdv.domain.enums.StatusVenda;
import br.com.loja.pdv.domain.model.ItemVenda;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.domain.model.Venda;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa um componente de infraestrutura com recursos controlados pelo teste. */
class FormatadorComprovanteTest {
    private final FormatadorComprovante formatter =
            new FormatadorComprovante("Loja Toninho");

    /** Verifica o cenario: deve formatar comprovante nao fiscal completo. */
    @Test
    void deveFormatarComprovanteNaoFiscalCompleto() {
        String text = formatter.formatar(sale(), false);
        assertTrue(text.contains("Loja Toninho"));
        assertTrue(text.contains("COMPROVANTE NÃO FISCAL"));
        assertTrue(text.contains("V20260728-ABC"));
        assertTrue(text.contains("Café"));
        assertTrue(text.contains("2 x"));
        assertTrue(text.contains("5,00"));
        assertTrue(text.contains("DINHEIRO"));
        assertTrue(text.contains("Valor recebido"));
        assertTrue(text.contains("Troco"));
        assertTrue(text.contains("R$ 5,00"));
        assertFalse(text.contains("\u00A0"));
        assertFalse(text.contains("SEGUNDA VIA"));
    }

    /** Verifica o cenario: deve identificar segunda via. */
    @Test
    void deveIdentificarSegundaVia() {
        String text = formatter.formatar(sale(), true);
        assertTrue(text.contains("SEGUNDA VIA"));
        assertTrue(text.contains("COMPROVANTE NÃO FISCAL"));
    }

    /** Verifica o cenario: deve usar valores historicos dos itens. */
    @Test
    void deveUsarValoresHistoricosDosItens() {
        Venda sale = sale();
        sale.getItens().getFirst().setPrecoUnitario(new BigDecimal("4.25"));
        sale.getItens().getFirst().setSubtotal(new BigDecimal("8.50"));
        String text = formatter.formatar(sale, false);
        assertTrue(text.contains("4,25"));
        assertTrue(text.contains("8,50"));
    }

    private Venda sale() {
        Venda sale = new Venda();
        sale.setNumero("V20260728-ABC");
        sale.setOperadorId(7);
        sale.setStatus(StatusVenda.FINALIZADA);
        sale.setCriadoEm(LocalDateTime.of(2026, 7, 28, 10, 30));
        sale.setSubtotal(new BigDecimal("10.00"));
        sale.setDesconto(BigDecimal.ZERO.setScale(2));
        sale.setTotal(new BigDecimal("10.00"));
        sale.setTroco(new BigDecimal("2.00"));

        ItemVenda item = new ItemVenda();
        item.setProdutoNome("Café");
        item.setQuantidade(2);
        item.setPrecoUnitario(new BigDecimal("5.00"));
        item.setSubtotal(new BigDecimal("10.00"));
        sale.getItens().add(item);

        Pagamento payment = new Pagamento();
        payment.setForma(FormaPagamento.DINHEIRO);
        payment.setValor(new BigDecimal("12.00"));
        sale.getPagamentos().add(payment);
        return sale;
    }
}
