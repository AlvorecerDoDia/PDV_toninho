# Estado atual do projeto

## Nucleo mantido

- login e usuarios simples;
- produtos e categorias;
- estoque por entrada e ajuste;
- venda com carrinho e um pagamento;
- caixa por abertura e fechamento;
- impressao e corte do comprovante;
- historico completo de vendas e produtos vendidos;
- cancelamento com devolucao de estoque e estorno de dinheiro;
- backup automatico ao sair;
- SQLite com migracoes.

## Recursos retirados para facilitar o estudo

- perfis e permissoes;
- troca obrigatoria de senha;
- pagamentos combinados;
- suprimento e sangria;
- relatorios e CSV;
- auditoria;
- backup e restauracao pela interface;
- instalador WiX.

## Regra de preservacao

O Historico permaneceu completo. A consulta de produtos vendidos continua aceitando
periodo e varias categorias, e os itens continuam guardando a categoria historica.
