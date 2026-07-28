package br.com.loja.pdv.domain.enums;

public enum TipoMovimentacaoEstoque {
    ENTRADA(1, false),
    AJUSTE_POSITIVO(1, true),
    AJUSTE_NEGATIVO(-1, true),
    SAIDA_VENDA(-1, false),
    DEVOLUCAO(1, false),
    PERDA(-1, true);

    private final int direction;
    private final boolean reasonRequired;

    TipoMovimentacaoEstoque(int direction, boolean reasonRequired) {
        this.direction = direction;
        this.reasonRequired = reasonRequired;
    }

    public int apply(int current, int quantity) {
        return Math.addExact(current, Math.multiplyExact(direction, quantity));
    }

    public boolean isReasonRequired() {
        return reasonRequired;
    }
}
