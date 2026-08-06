# Guia do codigo simplificado

## Arquitetura

```text
FXML e CSS
    -> Controller JavaFX
    -> Service
    -> Repository
    -> SQLiteRepository
    -> SQLite
```

- O FXML declara os componentes da tela.
- O controller le e atualiza os componentes.
- O service aplica validacoes e regras do fluxo.
- O repository define o contrato de persistencia.
- A implementacao SQLite executa SQL e transacoes.

## Ordem de leitura

1. `App.java`: cria repositorios, servicos e controllers.
2. `main-view.fxml` e `MainController.java`: navegacao principal.
3. `Produto`, `Categoria` e seus services: cadastro.
4. `EstoqueService`: entrada e ajuste do saldo.
5. `CarrinhoVenda`, `PagamentoService` e `VendaService`: venda.
6. `SQLiteVendaRepository`: transacao de venda e cancelamento.
7. Controllers e FXMLs de Historico: consultas preservadas.
8. `DatabaseMigrator`: evolucao do banco.

## Fluxo de cadastro

O menu Cadastro possui Produtos e Categorias. Cada produto aponta para uma
categoria. A quantidade inicial e aceita no cadastro; mudancas posteriores passam
pela tela Estoque.

## Fluxo de estoque

A interface oferece duas operacoes:

- entrada: soma uma quantidade ao saldo;
- ajuste: informa o saldo final desejado e um motivo.

Saidas por venda e devolucoes por cancelamento continuam automaticas.

## Fluxo de venda

1. O usuario adiciona produtos ao carrinho.
2. O sistema valida saldo, situacao e preco atual.
3. Uma forma de pagamento e escolhida.
4. Dinheiro pode gerar troco; pagamentos eletronicos usam o total exato.
5. Venda, item, estoque, pagamento e caixa sao gravados na mesma transacao.
6. O comprovante pode ser visualizado e impresso.

## Historico preservado

O Historico possui duas consultas:

- Vendas: periodo, operador, detalhes, pagamentos, cancelamento e segunda via.
- Produtos vendidos: periodo e filtro simultaneo por varias categorias.

O item da venda guarda `categoria_id` e `categoria_nome`. Assim, uma alteracao
futura no cadastro nao muda a classificacao exibida em vendas antigas.

## Caixa simplificado

O caixa possui apenas abertura, movimentos automaticos das vendas em dinheiro e
fechamento. Suprimento e sangria manuais nao fazem parte da interface simplificada.

## Usuarios simplificados

Todos os usuarios autenticados usam as mesmas telas. O modelo possui nome, login,
hash da senha e situacao. Nao existem perfis ou permissoes diferentes.

## Compatibilidade do banco

Algumas colunas e tabelas antigas continuam no esquema para que bancos existentes
abram sem perda de dados. O codigo simplificado apenas deixa de usar esses recursos.
As migracoes publicadas continuam numeradas de V001 a V008.
