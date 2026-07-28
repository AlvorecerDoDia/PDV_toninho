package br.com.loja.pdv.domain.enums;

import java.math.BigDecimal;

public enum TipoMovimentacaoCaixa {
    ABERTURA(1),
    VENDA_DINHEIRO(1),
    SUPRIMENTO(1),
    SANGRIA(-1),
    ESTORNO(-1);

    private final int sinal;

    TipoMovimentacaoCaixa(int sinal) {
        this.sinal = sinal;
    }

    public BigDecimal aplicar(BigDecimal saldo, BigDecimal valor) {
        return saldo.add(valor.multiply(BigDecimal.valueOf(sinal)));
    }
}
