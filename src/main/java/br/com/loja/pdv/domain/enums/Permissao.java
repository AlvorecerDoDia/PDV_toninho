package br.com.loja.pdv.domain.enums;

/** Acoes protegidas verificadas pela sessao antes de cada caso de uso. */
public enum Permissao {
    USUARIOS, PRODUTOS, PRECOS, ESTOQUE, VENDAS, CAIXA,
    RELATORIOS, CONFIGURACOES, BACKUP, DESCONTOS, CANCELAMENTOS,
    REIMPRESSAO, FECHAR_PROPRIO_CAIXA
}
