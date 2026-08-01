package br.com.loja.pdv.domain.enums;

/** Estados possiveis de uma sessao de caixa. */
public enum StatusCaixa {
    ABERTO,  // Aceita vendas e movimentacoes.
    FECHADO  // Mantem os valores finais apenas para consulta.
}
