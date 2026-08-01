package br.com.loja.pdv.domain.enums;

/** Consultas consolidadas disponiveis na tela de relatorios. */
public enum TipoRelatorio {
    VENDAS_POR_DIA,             // Totais agrupados por data.
    VENDAS_POR_PERIODO,         // Vendas individuais no intervalo.
    VENDAS_POR_OPERADOR,        // Desempenho por usuario.
    TOTAL_POR_FORMA_PAGAMENTO,  // Recebimentos agrupados pela forma.
    PRODUTOS_MAIS_VENDIDOS,     // Ranking por quantidade vendida.
    ESTOQUE_BAIXO,              // Produtos no limite minimo.
    MOVIMENTACOES_ESTOQUE,      // Entradas e saidas no periodo.
    DESCONTOS,                  // Vendas que receberam desconto.
    CANCELAMENTOS,              // Vendas canceladas e seus motivos.
    FECHAMENTO_CAIXA,           // Diferencas dos turnos fechados.
    LUCRO_BRUTO_ESTIMADO        // Receita menos custo historico estimado.
}
