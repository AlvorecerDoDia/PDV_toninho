package br.com.loja.pdv.domain.enums;

/** Ações protegidas verificadas pela sessão antes de cada caso de uso. */
public enum Permissao {
    USUARIOS, PRODUTOS, PRECOS, ESTOQUE, VENDAS, CAIXA,
    RELATORIOS, CONFIGURACOES, BACKUP, DESCONTOS, CANCELAMENTOS,
    REIMPRESSAO, FECHAR_PROPRIO_CAIXA
}
