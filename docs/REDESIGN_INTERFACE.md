# Redesign da interface

A interface foi reorganizada para reduzir poluição visual, deixar as ações mais previsíveis e facilitar o uso em operação de caixa.

## Principais mudanças

- navegação lateral dividida em Operação, Consultas, Gestão e Sistema;
- cabeçalho com usuário atual e situação do caixa sempre visíveis;
- formulários separados das tabelas de consulta;
- ações principais, secundárias e destrutivas com hierarquia visual consistente;
- cancelamento, fechamento e restauração isolados em áreas de risco;
- tela de venda dividida entre inclusão de produtos, carrinho, resumo e recebimento;
- textos de apoio próximos às decisões importantes;
- mensagens com espaço reservado para evitar mudanças bruscas no layout;
- campos e botões maiores, com foco mais visível;
- estados vazios nas tabelas para orientar o próximo passo.

## Heurísticas de Nielsen e Norman aplicadas

1. **Visibilidade do estado:** situação do caixa, usuário conectado, mensagens e totais permanecem visíveis.
2. **Correspondência com o mundo real:** nomes e agrupamentos seguem o fluxo de trabalho da loja.
3. **Controle e liberdade:** limpar filtros, limpar formulários e voltar a selecionar outra área são ações evidentes.
4. **Consistência:** botões, campos, cartões, tabelas e títulos seguem o mesmo padrão visual.
5. **Prevenção de erros:** operações destrutivas ficam separadas e destacadas.
6. **Reconhecimento em vez de memorização:** navegação, rótulos, dicas e estados vazios explicam as opções disponíveis.
7. **Flexibilidade e eficiência:** os atalhos da venda foram preservados.
8. **Design estético e minimalista:** informações relacionadas ficam juntas e ações raras deixam de competir com as principais.
9. **Recuperação de erros:** mensagens permanecem próximas da tarefa que originou o problema.
10. **Ajuda contextual:** instruções curtas aparecem nos formulários e nas áreas de risco.

## Validação realizada

- compilação de todo o código Java com JDK 21;
- validação da sintaxe XML dos arquivos FXML;
- validação estática de `fx:id`, `onAction`, controllers e arquivos incluídos;
- verificação de comentários sem acentos nos arquivos Java, FXML, CSS, SQL e PowerShell;
- verificação de whitespace com `git diff --check`.

A suíte Maven completa não pôde ser executada neste ambiente porque o Maven Wrapper tentou baixar sua distribuição e o acesso externo estava indisponível. O código Java foi compilado diretamente com as dependências já presentes no projeto.

## Ajuste do cadastro de produtos

- Quantidade inicial disponivel durante o primeiro cadastro.
- Saldo exibido na tabela de produtos.
- Campo bloqueado durante a edicao; ajustes posteriores permanecem na tela Estoque.
