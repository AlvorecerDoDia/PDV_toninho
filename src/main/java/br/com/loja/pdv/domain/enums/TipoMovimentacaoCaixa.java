package br.com.loja.pdv.domain.enums;

import java.math.BigDecimal;

/** Tipos que determinam como um valor afeta o dinheiro esperado do caixa. */
public enum TipoMovimentacaoCaixa {
    ABERTURA(1),
    VENDA_DINHEIRO(1),
    SUPRIMENTO(1),
    SANGRIA(-1),
    ESTORNO(-1);

    private final int sinal;

    /** Guarda o sinal usado ao somar a movimentacao ao dinheiro esperado. */
    TipoMovimentacaoCaixa(int sinal) {
        this.sinal = sinal;
    }

    /** Aplica o sinal da movimentacao ao saldo informado. */
    public BigDecimal aplicar(BigDecimal saldo, BigDecimal valor) {
        return saldo.add(valor.multiply(BigDecimal.valueOf(sinal)));
    }
}
