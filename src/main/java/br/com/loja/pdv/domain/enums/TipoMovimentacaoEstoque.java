package br.com.loja.pdv.domain.enums;

/** Tipos que determinam se uma movimentacao soma ou subtrai estoque. */
public enum TipoMovimentacaoEstoque {
    ENTRADA(1, false),
    AJUSTE_POSITIVO(1, true),
    AJUSTE_NEGATIVO(-1, true),
    SAIDA_VENDA(-1, false),
    DEVOLUCAO(1, false),
    PERDA(-1, true);

    private final int direction;
    private final boolean reasonRequired;

    /** Define o efeito no saldo e se a justificativa e obrigatoria. */
    TipoMovimentacaoEstoque(int direction, boolean reasonRequired) {
        this.direction = direction;
        this.reasonRequired = reasonRequired;
    }

    /** Calcula o saldo posterior usando o efeito positivo ou negativo do tipo. */
    public int apply(int current, int quantity) {
        return Math.addExact(current, Math.multiplyExact(direction, quantity));
    }

    /** Informa se o tipo exige justificativa do operador. */
    public boolean isReasonRequired() {
        return reasonRequired;
    }
}
