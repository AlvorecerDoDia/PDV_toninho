package br.com.loja.pdv.domain.enums;

/** Formas de recebimento aceitas na primeira versao do PDV. */
public enum FormaPagamento {
    DINHEIRO,       // Permite calcular troco e movimenta o caixa fisico.
    PIX,            // Recebimento digital registrado sem integracao bancaria.
    CARTAO_DEBITO,  // Pagamento registrado como debito.
    CARTAO_CREDITO  // Pagamento registrado como credito.
}
