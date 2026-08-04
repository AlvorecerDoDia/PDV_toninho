# Guia de leitura do codigo

Este documento explica como o PDV Toninho foi organizado e sugere uma ordem de
leitura. Os comentarios dentro do codigo foram escritos sem acentos para evitar
problemas em ferramentas antigas, mas os textos apresentados ao usuario continuam
em portugues normal.

## 1. Visao geral da arquitetura

O projeto usa uma separacao em camadas:

```text
FXML e CSS
    ↓
Controller JavaFX
    ↓
Service (regras de negocio)
    ↓
Repository (contrato de persistencia)
    ↓
SQLiteRepository (SQL e transacoes)
    ↓
Banco SQLite
```

Cada camada possui uma responsabilidade clara:

- **FXML:** declara os componentes visuais e liga botoes aos metodos do controller.
- **CSS:** define aparencia, hierarquia visual e estados dos componentes.
- **Controller:** le campos, chama servicos e atualiza a tela.
- **Service:** valida permissoes, entradas e regras de negocio.
- **Repository:** define as operacoes de persistencia sem depender do SQLite.
- **SQLiteRepository:** executa SQL, converte resultados e controla transacoes.
- **Model:** transporta os dados entre as camadas.

A regra mais importante para entender a base e esta: **controller nao executa SQL
e repository nao decide regra de negocio de interface**.

## 2. Ordem recomendada de leitura

Para aprender o projeto sem se perder, siga esta sequencia:

1. `pom.xml` — tecnologias, versoes e processo de compilacao.
2. `module-info.java` — modulos Java utilizados e pacotes abertos ao FXML.
3. `App.java` — montagem das dependencias e troca entre login e tela principal.
4. `main-view.fxml` e `MainController.java` — navegacao e permissoes visuais.
5. `Produto.java`, `ProdutoService.java` e `SQLiteProdutoRepository.java` — fluxo
   CRUD mais simples da aplicacao.
6. `CarrinhoVenda.java`, `VendaService.java` e `SQLiteVendaRepository.java` —
   fluxo transacional mais importante.
7. `CaixaService.java` e `SQLiteCaixaRepository.java` — dinheiro esperado,
   abertura, sangria, suprimento e fechamento.
8. `DatabaseMigrator.java` e os arquivos `V00X__*.sql` — evolucao do banco.
9. Testes do mesmo componente que acabou de ser lido.

## 3. Inicializacao da aplicacao

O executavel comeca em `PdvLauncher.main`. Essa classe chama o JavaFX e evita um
problema comum quando uma classe que herda `Application` e usada diretamente por
um pacote criado com `jpackage`.

O JavaFX cria `App` e executa estas etapas:

1. `App.init` configura logs, banco, migracoes, repositorios e servicos.
2. `App.start` configura a janela e abre o login.
3. `App.showLogin` carrega `login-view.fxml` e injeta `LoginController`.
4. Depois do login, `App.showMain` carrega `main-view.fxml`.
5. `App.createMainController` fornece a cada controller apenas as dependencias de
   que ele precisa.
6. `App.stop` tenta criar um backup automatico no encerramento normal.

Esse processo funciona como uma injecao de dependencias manual. Nao existe Spring
ou outro container: a propria classe `App` monta o grafo de objetos.

## 4. Interface JavaFX

### FXML

Cada arquivo FXML possui:

- `fx:controller`, que indica a classe responsavel pela tela;
- `fx:id`, que liga um componente a um campo anotado com `@FXML`;
- `onAction`, que liga um botao a um metodo do controller;
- `fx:include`, usado para incluir uma tela dentro de outra.

Exemplo do fluxo de um botao:

```text
<Button onAction="#save">
        ↓
ProdutoController.save()
        ↓
ProdutoService.cadastrar() ou atualizar()
        ↓
ProdutoRepository
        ↓
SQLiteProdutoRepository
```

### CSS

`app.css` centraliza o estilo. As classes mais importantes sao:

- `.page-shell`: estrutura externa de cada pagina;
- `.card`: agrupa uma tarefa relacionada;
- `.primary`: acao principal;
- `.danger`: acao destrutiva;
- `.ghost`: acao secundaria discreta;
- `.feedback`: reserva espaco para mensagens;
- `.nav-button`: opcoes da navegacao lateral;
- `.status-chip`: estado do usuario ou do caixa.

## 5. Login, sessao e permissoes

O fluxo de autenticacao envolve:

- `LoginController`: recebe login, senha e nova senha quando obrigatoria;
- `AutenticacaoService`: verifica usuario ativo e hash da senha;
- `PasswordHasher`: cria e compara hashes PBKDF2;
- `SessaoUsuario`: guarda apenas o usuario autenticado em memoria;
- `PerfilUsuario`: agrupa as permissoes de cada perfil;
- `MainController`: remove da navegacao as telas sem permissao;
- servicos: chamam `sessao.exigir(...)` antes de casos de uso protegidos.

A interface ajuda o usuario, mas a seguranca nao depende apenas dela. Mesmo que
um metodo fosse chamado sem o botao correspondente, o service ainda validaria a
permissao.

## 6. Cadastro de produtos

O cadastro percorre estas etapas:

1. `ProdutoController.save` converte os campos da tela em um `Produto`.
2. Se nao existe item selecionado, chama `ProdutoService.cadastrar`.
3. O service normaliza nome e codigo, valida valores e prepara datas.
4. `SQLiteProdutoRepository.salvar` converte dinheiro para centavos e executa o
   `INSERT`.
5. A chave gerada pelo SQLite volta para o objeto.
6. O controller limpa o formulario e recarrega a tabela.

A quantidade inicial e aceita somente no primeiro cadastro. Durante edicoes,
`ProdutoService.atualizar` recupera o produto persistido e preserva o saldo. Isso
obriga alteracoes posteriores a passarem pela tela de estoque, onde existe
historico e motivo.

## 7. Estoque

`TipoMovimentacaoEstoque` define se uma movimentacao soma ou subtrai saldo e se
exige justificativa.

`EstoqueService.registrar` valida:

- permissao;
- existencia do produto;
- quantidade positiva;
- motivo quando obrigatorio;
- usuario autenticado.

`SQLiteEstoqueRepository.registrar` abre uma transacao e realiza quatro passos:

1. le o saldo atual;
2. calcula o saldo posterior;
3. atualiza o produto;
4. grava `movimentacao_estoque` com saldo anterior e posterior.

Se qualquer passo falhar, o rollback impede que o saldo seja alterado sem o
historico correspondente.

## 8. Carrinho e venda

### Carrinho em memoria

`CarrinhoVenda` existe somente durante a venda atual. Ele:

- adiciona ou soma produtos;
- altera quantidades;
- valida estoque conhecido;
- calcula subtotal;
- aplica desconto;
- calcula total.

O carrinho nao grava no banco. Ele prepara uma intencao de venda.

### Finalizacao

`VendaService.finalizar`:

1. valida permissao e carrinho nao vazio;
2. exige permissao de desconto quando necessario;
3. confirma que o operador possui caixa aberto;
4. valida pagamentos e calcula troco;
5. cria o cabecalho da venda;
6. copia os itens para valores historicos;
7. delega a transacao ao repositorio.

`SQLiteVendaRepository.finalizar` repete validacoes sensiveis dentro da transacao,
porque estoque, preco ou caixa podem ter mudado depois que o produto entrou no
carrinho. Na mesma transacao ele:

- insere a venda;
- insere os itens;
- baixa estoque;
- grava movimentacoes de estoque;
- insere pagamentos;
- movimenta o dinheiro do caixa;
- registra auditoria de desconto.

Somente depois de todas as etapas o commit e executado.

## 9. Pagamentos e troco

Uma venda pode ter varias instancias de `Pagamento`. `PagamentoService` exige que
cada valor seja positivo e tenha no maximo duas casas decimais.

O total recebido precisa cobrir o total da venda. O excesso somente pode ser
explicado por dinheiro, pois PIX e cartao nao geram troco dentro do sistema.

A movimentacao de caixa considera:

```text
dinheiro retido = dinheiro recebido - troco
```

Esse valor, e nao o total completo da venda, e registrado como
`VENDA_DINHEIRO`.

## 10. Caixa

`CaixaService` controla as regras e `SQLiteCaixaRepository` garante consistencia
no banco.

- **Abertura:** cria o caixa e uma movimentacao `ABERTURA`.
- **Suprimento:** adiciona dinheiro fisico.
- **Sangria:** retira dinheiro fisico e exige motivo.
- **Venda em dinheiro:** adiciona apenas o valor retido.
- **Estorno:** remove o dinheiro retido de uma venda cancelada.
- **Fechamento:** compara valor contado com valor esperado.

O valor esperado e calculado a partir das movimentacoes, usando o sinal definido
em `TipoMovimentacaoCaixa`.

## 11. Cancelamento de venda

Cancelar nao apaga a venda. O historico permanece com status `CANCELADA`.

A transacao de cancelamento:

1. confirma que a venda ainda esta finalizada;
2. grava data e motivo;
3. devolve cada item ao estoque;
4. grava movimentacoes `DEVOLUCAO`;
5. calcula o dinheiro retido originalmente;
6. registra `ESTORNO` no caixa quando necessario;
7. corrige valores de caixa ja fechado;
8. grava auditoria.

Qualquer erro desfaz todas as etapas.

## 12. Banco e migracoes

`Database` cria uma conexao por operacao e aplica:

- `foreign_keys = ON` para chaves estrangeiras;
- `busy_timeout` para aguardar bloqueios curtos;
- `journal_mode = WAL` para melhorar leitura e escrita local.

`DatabaseMigrator` cria `schema_version`, le versoes aplicadas e executa cada
arquivo SQL pendente. Uma versao somente e registrada depois que seu script foi
executado com sucesso.

As migracoes nunca devem ser renumeradas depois de publicadas. Uma mudanca nova
deve receber o proximo numero.

## 13. Dinheiro

A interface e o dominio usam `BigDecimal`. O banco usa centavos inteiros.

`MoneyUtils.toCents` transforma, por exemplo, `10.25` em `1025` sem arredondar
silenciosamente. `MoneyUtils.fromCents` faz o caminho inverso.

Essa estrategia evita erros comuns de `double`, como valores aproximados em
operacoes financeiras.

### Impressao termica e codificacao

`FormatadorComprovante` cria o texto usado na visualizacao e troca o espaco nao
separavel produzido pelo formato monetario brasileiro por um espaco comum.

`CodificadorComprovante` prepara a versao enviada fisicamente para a impressora.
Ele remove acentos, normaliza sinais tipograficos e gera bytes ASCII. Essa etapa
e necessaria porque muitas impressoras termicas recebem dados brutos e tratam
UTF-8 como CP850 ou outra pagina de codigo, produzindo caracteres estranhos.

`ImpressoraWindows` usa o texto completo do formatador, passa pelo codificador e
somente depois cria o trabalho de impressao. A visualizacao continua com Unicode;
a limitacao ASCII existe apenas na saida fisica mais compativel.

## 14. Auditoria e logs

Auditoria e log possuem objetivos diferentes:

- **Auditoria:** registra quem realizou uma acao de negocio relevante.
- **Log:** registra detalhes tecnicos para diagnostico.

`AuditoriaService` evita incluir senha ou hash nos valores gravados.
`ErrorHandler` mostra mensagens seguras na interface e envia a excecao completa
para `java.util.logging`.

## 15. Backup

`GerenciadorBackup.criar` usa o comando SQLite `VACUUM INTO`, que produz uma
copia consistente mesmo com o banco em uso.

Na restauracao:

1. o arquivo escolhido e validado;
2. o banco atual recebe um backup de seguranca;
3. o arquivo e copiado para um caminho temporario;
4. a troca e feita de forma atomica;
5. arquivos WAL e SHM antigos sao removidos.

## 16. Relatorios

`RelatorioService` valida permissao e periodo. `SQLiteRelatorioRepository` escolhe
uma consulta conforme `TipoRelatorio` e converte diferentes resultados para
`LinhaRelatorio`.

O modelo generico permite usar uma unica tabela JavaFX para relatorios com
colunas opcionais de texto, quantidade, valores e data.

`ExportadorCsv` protege celulas que comecam com caracteres interpretados por
planilhas como formula.

## 17. Testes

Os testes seguem a mesma separacao das camadas:

- `domain`: regras puras do carrinho;
- `service`: validacoes e permissoes;
- `repository`: SQL e transacoes em banco temporario;
- `infrastructure`: migracao, backup, impressao e caminhos;
- `integration`: fluxo completo e carregamento de FXML.

Os nomes dos metodos descrevem o comportamento esperado. Os comentarios antes de
cada `@Test` apresentam o cenario em linguagem direta.

Ao estudar uma classe, abra seu teste correspondente em seguida. Isso mostra quais
regras sao consideradas essenciais e como as dependencias podem ser substituidas.

## 18. Convencoes usadas no codigo

- textos apresentados ao usuario podem possuir acentos;
- comentarios de codigo usam somente ASCII;
- dinheiro nunca usa `double`;
- SQL operacional fica apenas em repositorios SQLite;
- operacoes compostas usam transacao e rollback;
- entidades apagadas logicamente usam status em vez de exclusao fisica;
- data e hora sao injetadas por `Clock` em servicos testaveis;
- `Optional` representa resultados que podem nao existir;
- excecoes de validacao podem ser mostradas ao operador;
- excecoes de banco escondem SQL e detalhes internos.

## 19. Como acompanhar uma acao no codigo

Quando quiser entender um comportamento da interface:

1. localize o texto do botao no FXML;
2. veja o valor de `onAction`;
3. abra o metodo de mesmo nome no controller;
4. identifique qual metodo do service e chamado;
5. leia as validacoes do service;
6. abra a interface de repository;
7. abra a implementacao SQLite;
8. leia o teste do service e o teste do repository.

Esse caminho funciona para praticamente todas as funcionalidades do projeto.

## 20. Glossario rapido

- **Controller:** adaptador entre componentes JavaFX e regras de negocio.
- **Service:** caso de uso e validacao de negocio.
- **Repository:** porta de acesso aos dados.
- **Model:** objeto que representa dados do dominio.
- **DTO/record:** estrutura curta e geralmente imutavel para transportar dados.
- **FXML:** XML que declara a arvore visual JavaFX.
- **JDBC:** API usada para enviar SQL ao banco.
- **Transacao:** grupo de operacoes que confirma tudo ou desfaz tudo.
- **Commit:** confirma uma transacao no banco.
- **Rollback:** desfaz uma transacao com falha.
- **Hash:** representacao irreversivel usada para verificar senha.
- **Salt:** valor aleatorio que protege hashes iguais de senhas iguais.
- **WAL:** modo de diario do SQLite que separa gravacoes temporarias.
## 21. Indice dos arquivos principais

### Codigo Java de producao

| Arquivo | Responsabilidade |
|---|---|
| `src/main/java/br/com/loja/pdv/App.java` | Monta as dependencias e controla a troca entre login e tela principal. |
| `src/main/java/br/com/loja/pdv/PdvLauncher.java` | Inicializador separado necessario para o JavaFX funcionar no pacote jpackage. |
| `src/main/java/br/com/loja/pdv/config/AppPaths.java` | Resolve todos os arquivos gravaveis fora da pasta de instalacao. |
| `src/main/java/br/com/loja/pdv/controller/BackupController.java` | Apresenta criacao, listagem e restauracao de backups ao administrador. |
| `src/main/java/br/com/loja/pdv/controller/CaixaController.java` | Coordena a tela de abertura, movimentacoes e fechamento de caixa. |
| `src/main/java/br/com/loja/pdv/controller/ErrorHandler.java` | Converte falhas tecnicas em mensagens seguras e registra os detalhes no log. |
| `src/main/java/br/com/loja/pdv/controller/EstoqueController.java` | Liga os campos de estoque ao historico e as regras do servico. |
| `src/main/java/br/com/loja/pdv/controller/HistoricoVendaController.java` | Controla pesquisa, detalhes, cancelamento e reimpressao de vendas. |
| `src/main/java/br/com/loja/pdv/controller/LoginController.java` | Autentica o usuario e conduz a troca obrigatoria da senha temporaria. |
| `src/main/java/br/com/loja/pdv/controller/MainController.java` | Monta a navegacao permitida pelo perfil e exibe o estado da sessao. |
| `src/main/java/br/com/loja/pdv/controller/PagamentoController.java` | Gerencia as formas de pagamento antes de solicitar a finalizacao da venda. |
| `src/main/java/br/com/loja/pdv/controller/ProdutoController.java` | Controla o formulario e a tabela do cadastro de produtos. |
| `src/main/java/br/com/loja/pdv/controller/RelatorioController.java` | Aplica filtros, exibe resultados e exporta relatorios para CSV. |
| `src/main/java/br/com/loja/pdv/controller/UiFormatters.java` | Instala filtros de entrada reutilizaveis nos campos JavaFX. |
| `src/main/java/br/com/loja/pdv/controller/UsuarioController.java` | Controla cadastro e manutencao dos usuarios e perfis. |
| `src/main/java/br/com/loja/pdv/controller/VendaController.java` | Mantem a interacao rapida com o carrinho e os atalhos da tela de venda. |
| `src/main/java/br/com/loja/pdv/domain/enums/FormaPagamento.java` | Formas de recebimento aceitas na primeira versao do PDV. |
| `src/main/java/br/com/loja/pdv/domain/enums/PerfilUsuario.java` | Perfis que agrupam as permissoes disponiveis no sistema. |
| `src/main/java/br/com/loja/pdv/domain/enums/Permissao.java` | Acoes protegidas verificadas pela sessao antes de cada caso de uso. |
| `src/main/java/br/com/loja/pdv/domain/enums/StatusCaixa.java` | Estados possiveis de uma sessao de caixa. |
| `src/main/java/br/com/loja/pdv/domain/enums/StatusVenda.java` | Estados persistidos de uma venda. |
| `src/main/java/br/com/loja/pdv/domain/enums/TipoMovimentacaoCaixa.java` | Tipos que determinam como um valor afeta o dinheiro esperado do caixa. |
| `src/main/java/br/com/loja/pdv/domain/enums/TipoMovimentacaoEstoque.java` | Tipos que determinam se uma movimentacao soma ou subtrai estoque. |
| `src/main/java/br/com/loja/pdv/domain/enums/TipoRelatorio.java` | Consultas consolidadas disponiveis na tela de relatorios. |
| `src/main/java/br/com/loja/pdv/domain/model/Categoria.java` | Categoria persistida usada para organizar o catalogo. |
| `src/main/java/br/com/loja/pdv/domain/model/Caixa.java` | Representa a abertura e o eventual fechamento do caixa de um operador. |
| `src/main/java/br/com/loja/pdv/domain/model/CarrinhoVenda.java` | Agregado em memoria que calcula itens, subtotal, desconto e total da venda. |
| `src/main/java/br/com/loja/pdv/domain/model/FiltroRelatorio.java` | Filtros opcionais e periodo obrigatorio usados pelas consultas de relatorio. |
| `src/main/java/br/com/loja/pdv/domain/model/ItemCarrinho.java` | Item mutavel do carrinho com preco capturado no momento da inclusao. |
| `src/main/java/br/com/loja/pdv/domain/model/ItemVenda.java` | Item persistido com nome, custo e preco historicos da venda. |
| `src/main/java/br/com/loja/pdv/domain/model/LinhaRelatorio.java` | Linha generica que permite exibir diferentes consolidacoes na mesma tabela. |
| `src/main/java/br/com/loja/pdv/domain/model/MovimentacaoCaixa.java` | Registro imutavel apos persistencia de uma entrada ou saida do caixa. |
| `src/main/java/br/com/loja/pdv/domain/model/MovimentacaoEstoque.java` | Historico de uma alteracao de estoque com saldos anterior e posterior. |
| `src/main/java/br/com/loja/pdv/domain/model/Pagamento.java` | Parcela de pagamento associada a uma forma de recebimento. |
| `src/main/java/br/com/loja/pdv/domain/model/Produto.java` | Produto vendavel, incluindo precos, estoque atual e limite minimo. |
| `src/main/java/br/com/loja/pdv/domain/model/RegistroAuditoria.java` | Evidencia de uma acao critica com autor, entidade e valores relevantes. |
| `src/main/java/br/com/loja/pdv/domain/model/Usuario.java` | Usuario autenticavel com perfil, status e hash de senha. |
| `src/main/java/br/com/loja/pdv/domain/model/Venda.java` | Agregado persistido que reune cabecalho, itens e pagamentos da venda. |
| `src/main/java/br/com/loja/pdv/exception/DatabaseException.java` | Sinaliza falha tecnica de persistencia sem expor SQL ao usuario. |
| `src/main/java/br/com/loja/pdv/exception/DuplicateBarcodeException.java` | Informa que um codigo de barras ja pertence a outro produto. |
| `src/main/java/br/com/loja/pdv/exception/EntityNotFoundException.java` | Indica que a entidade solicitada deixou de existir ou nunca existiu. |
| `src/main/java/br/com/loja/pdv/exception/ImpressaoException.java` | Representa indisponibilidade ou falha da impressora sem invalidar a venda. |
| `src/main/java/br/com/loja/pdv/exception/ValidationException.java` | Erro de regra de negocio que pode ser mostrado diretamente ao operador. |
| `src/main/java/br/com/loja/pdv/infrastructure/backup/GerenciadorBackup.java` | Executa backup SQLite consistente, validacao, retencao e restauracao. |
| `src/main/java/br/com/loja/pdv/infrastructure/database/Database.java` | Abre conexoes SQLite ja configuradas com integridade e espera por bloqueios. |
| `src/main/java/br/com/loja/pdv/infrastructure/database/DatabaseInitializer.java` | Fachada curta usada pela aplicacao e pelos testes para aplicar migracoes. |
| `src/main/java/br/com/loja/pdv/infrastructure/database/DatabaseMigrator.java` | Descobre e aplica scripts versionados uma unica vez, dentro de transacao. |
| `src/main/java/br/com/loja/pdv/infrastructure/logging/LoggingConfigurator.java` | Instala os handlers de console e arquivo rotativo do java.util.logging. |
| `src/main/java/br/com/loja/pdv/infrastructure/printing/CodificadorComprovante.java` | Normaliza o texto e gera bytes ASCII compativeis com impressoras termicas. |
| `src/main/java/br/com/loja/pdv/infrastructure/printing/FormatadorComprovante.java` | Constroi o texto do comprovante usando apenas valores historicos da venda. |
| `src/main/java/br/com/loja/pdv/infrastructure/printing/ImpressoraComprovante.java` | Porta de impressao que permite trocar ou simular a impressora fisica. |
| `src/main/java/br/com/loja/pdv/infrastructure/printing/ImpressoraWindows.java` | Envia o comprovante textual a impressora padrao registrada no Windows. |
| `src/main/java/br/com/loja/pdv/infrastructure/reporting/ExportadorCsv.java` | Serializa linhas de relatorio em CSV UTF-8 compativel com planilhas. |
| `src/main/java/br/com/loja/pdv/infrastructure/security/PasswordHasher.java` | Deriva e verifica hashes PBKDF2-SHA256 com salt aleatorio por senha. |
| `src/main/java/br/com/loja/pdv/repository/AuditoriaRepository.java` | Contrato de gravacao e consulta dos eventos de auditoria. |
| `src/main/java/br/com/loja/pdv/repository/CategoriaRepository.java` | Contrato CRUD das categorias de produtos. |
| `src/main/java/br/com/loja/pdv/repository/CaixaRepository.java` | Contrato transacional de caixas e suas movimentacoes financeiras. |
| `src/main/java/br/com/loja/pdv/repository/EstoqueRepository.java` | Contrato de alteracao atomica e consulta do estoque. |
| `src/main/java/br/com/loja/pdv/repository/PagamentoRepository.java` | Contrato de consulta dos pagamentos persistidos por venda. |
| `src/main/java/br/com/loja/pdv/repository/ProdutoRepository.java` | Contrato CRUD e de pesquisa do catalogo de produtos. |
| `src/main/java/br/com/loja/pdv/repository/RelatorioRepository.java` | Contrato das consultas consolidadas usadas nos relatorios. |
| `src/main/java/br/com/loja/pdv/repository/UsuarioRepository.java` | Contrato de persistencia e autenticacao dos usuarios. |
| `src/main/java/br/com/loja/pdv/repository/VendaRepository.java` | Contrato transacional de finalizacao, consulta e cancelamento de vendas. |
| `src/main/java/br/com/loja/pdv/repository/sqlite/SQLiteAuditoriaRepository.java` | Implementa a auditoria com comandos JDBC preparados. |
| `src/main/java/br/com/loja/pdv/repository/sqlite/SQLiteCategoriaRepository.java` | Persiste as categorias usadas pelo catalogo. |
| `src/main/java/br/com/loja/pdv/repository/sqlite/SQLiteCaixaRepository.java` | Persiste caixa e movimentacoes preservando consistencia transacional. |
| `src/main/java/br/com/loja/pdv/repository/sqlite/SQLiteEstoqueRepository.java` | Atualiza saldo e historico de estoque na mesma transacao SQLite. |
| `src/main/java/br/com/loja/pdv/repository/sqlite/SQLitePagamentoRepository.java` | Consulta os pagamentos historicos associados a uma venda. |
| `src/main/java/br/com/loja/pdv/repository/sqlite/SQLiteProdutoRepository.java` | Implementa o catalogo de produtos e traduz restricoes do SQLite. |
| `src/main/java/br/com/loja/pdv/repository/sqlite/SQLiteRelatorioRepository.java` | Executa as consultas agregadas e converte centavos para BigDecimal. |
| `src/main/java/br/com/loja/pdv/repository/sqlite/SQLiteUsuarioRepository.java` | Persiste usuarios sem nunca armazenar a senha original. |
| `src/main/java/br/com/loja/pdv/repository/sqlite/SQLiteVendaRepository.java` | Executa venda e cancelamento como transacoes completas do SQLite. |
| `src/main/java/br/com/loja/pdv/service/AuditoriaService.java` | Registra acoes relevantes do usuario para rastreabilidade administrativa. |
| `src/main/java/br/com/loja/pdv/service/AutenticacaoService.java` | Autentica usuarios ativos e mantem a sessao da aplicacao sincronizada. |
| `src/main/java/br/com/loja/pdv/service/BackupService.java` | Coordena criacao, retencao e restauracao de backups conforme as permissoes do usuario. |
| `src/main/java/br/com/loja/pdv/service/CategoriaService.java` | Centraliza cadastro, consulta e situacao das categorias. |
| `src/main/java/br/com/loja/pdv/service/CaixaService.java` | Aplica as regras de abertura, movimentacao e fechamento do caixa. |
| `src/main/java/br/com/loja/pdv/service/EstoqueService.java` | Valida entradas, saidas e ajustes de estoque antes de persistir cada movimentacao. |
| `src/main/java/br/com/loja/pdv/service/PagamentoService.java` | Monta pagamentos, valida os valores recebidos e calcula o troco da venda. |
| `src/main/java/br/com/loja/pdv/service/ProdutoService.java` | Centraliza cadastro, edicao, consulta e inativacao de produtos. |
| `src/main/java/br/com/loja/pdv/service/RelatorioService.java` | Valida acesso e periodo antes de solicitar a geracao dos relatorios. |
| `src/main/java/br/com/loja/pdv/service/SessaoUsuario.java` | Mantem o usuario autenticado em memoria e verifica suas permissoes. |
| `src/main/java/br/com/loja/pdv/service/UsuarioService.java` | Gerencia usuarios, credenciais e a configuracao segura do acesso administrativo inicial. |
| `src/main/java/br/com/loja/pdv/service/VendaService.java` | Orquestra a validacao, finalizacao e o cancelamento das vendas do PDV. |
| `src/main/java/br/com/loja/pdv/util/MoneyUtils.java` | Converte valores monetarios entre BigDecimal e centavos inteiros sem arredondar. |
| `src/main/java/module-info.java` | Componente Java do projeto. |

### Telas FXML

| Arquivo | Responsabilidade |
|---|---|
| `src/main/resources/br/com/loja/pdv/view/backup-view.fxml` | Backup com criacao e restauracao visualmente separadas. |
| `src/main/resources/br/com/loja/pdv/view/caixa-view.fxml` | Caixa organizado por estado, operacoes e historico. |
| `src/main/resources/br/com/loja/pdv/view/estoque-view.fxml` | Movimentacao de estoque com saldo e historico em areas separadas. |
| `src/main/resources/br/com/loja/pdv/view/historico-venda-view.fxml` | Consulta de vendas com acoes comuns e cancelamento separados. |
| `src/main/resources/br/com/loja/pdv/view/login-view.fxml` | Entrada no sistema com foco na autenticacao. |
| `src/main/resources/br/com/loja/pdv/view/main-view.fxml` | Estrutura principal com navegacao por contexto e estado da sessao. |
| `src/main/resources/br/com/loja/pdv/view/pagamento-view.fxml` | Composicao dos pagamentos e conclusao da venda. |
| `src/main/resources/br/com/loja/pdv/view/produto-view.fxml` | Cadastro e consulta do catalogo com tarefas separadas. |
| `src/main/resources/br/com/loja/pdv/view/relatorio-view.fxml` | Relatorios com filtros identificados e acoes priorizadas. |
| `src/main/resources/br/com/loja/pdv/view/usuario-view.fxml` | Usuarios com formulario, permissoes e lista em areas separadas. |
| `src/main/resources/br/com/loja/pdv/view/venda-view.fxml` | Fluxo de venda organizado por entrada, carrinho e recebimento. |

### Migracoes SQL

| Arquivo | Responsabilidade |
|---|---|
| `src/main/resources/br/com/loja/pdv/database/migration/V001__cria_tabela_produto.sql` | Estrutura principal do catalogo e controle do saldo atual. |
| `src/main/resources/br/com/loja/pdv/database/migration/V002__cria_movimentacao_estoque.sql` | Historico imutavel de entradas, saidas, perdas, ajustes e devolucoes. |
| `src/main/resources/br/com/loja/pdv/database/migration/V003__cria_usuario.sql` | Usuarios locais, perfis de acesso e credenciais armazenadas por hash. |
| `src/main/resources/br/com/loja/pdv/database/migration/V004__cria_caixa.sql` | Sessoes de caixa e lancamentos financeiros vinculados ao operador. |
| `src/main/resources/br/com/loja/pdv/database/migration/V005__cria_venda.sql` | Cabecalho, itens e pagamentos que preservam os valores historicos da venda. |
| `src/main/resources/br/com/loja/pdv/database/migration/V006__cria_auditoria.sql` | Registro das acoes administrativas e alteracoes sensiveis. |
| `src/main/resources/br/com/loja/pdv/database/migration/V007__cria_categoria_produto.sql` | Cria categorias e associa cada produto a uma categoria. |

## 22. Arquivos que normalmente nao devem ser editados

- `mvnw` e `mvnw.cmd`: scripts gerados pelo Maven Wrapper.
- `.mvn/wrapper/maven-wrapper.jar`: executavel do wrapper.
- arquivos dentro de `target/`: resultados de compilacao e empacotamento.
- banco `pdv.db`, arquivos `-wal` e `-shm`: dados locais de execucao.

Ao alterar uma funcionalidade, prefira modificar fonte, recurso, migracao nova e teste correspondente.


## Categorias de produtos

A categoria e uma entidade persistida, nao um enum. Cada produto guarda uma unica
chave `categoria_id`, enquanto uma categoria pode organizar varios produtos.

O fluxo principal passa por:

1. `V007__cria_categoria_produto.sql`, que cria e popula a tabela `categoria`;
2. `CategoriaService`, que normaliza nomes e controla a situacao;
3. `SQLiteCategoriaRepository`, que executa o CRUD no SQLite;
4. `ProdutoService`, que exige uma categoria ativa no cadastro real;
5. `ProdutoController`, que preenche o `ComboBox<Categoria>` e a coluna da tabela;
6. `SQLiteProdutoRepository`, que grava `categoria_id` e recupera os dados com `JOIN`.

Produtos de bancos antigos recebem `Sem categoria` durante a migracao. O operador
pode depois editar o produto e escolher a classificacao correta.
