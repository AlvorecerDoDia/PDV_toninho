# PDV Toninho

Sistema de ponto de venda local para Windows, voltado a uma única estação de
trabalho. A aplicação controla produtos, estoque, usuários, caixa, vendas,
pagamentos, comprovantes, cancelamentos, relatórios, auditoria e backups.

> O comprovante emitido é **não fiscal**. O sistema não emite NFC-e, não se
> comunica com a SEFAZ e não substitui uma solução fiscal homologada.

## Funcionalidades

- cadastro com categoria e quantidade inicial, pesquisa, edição, ativação e desativação de produtos;
- estoque transacional com entradas, ajustes, perdas, saídas e devoluções;
- perfis Administrador, Gerente e Operador, com senha armazenada por hash;
- abertura, suprimento, sangria e fechamento de caixa;
- carrinho, desconto autorizado e venda com dinheiro, PIX e cartões;
- pagamentos combinados e cálculo de troco exclusivamente sobre dinheiro;
- comprovante não fiscal, visualização, impressão e segunda via;
- histórico, cancelamento transacional e estorno de estoque/caixa;
- relatórios operacionais e exportação CSV;
- auditoria de ações críticas e logs rotativos;
- backup manual, backup automático ao sair e restauração segura.

## Tecnologias

Java 21, JavaFX 21, FXML, Maven, SQLite/JDBC e JUnit 5.

## Requisitos para desenvolvimento

1. Windows 10 ou 11.
2. JDK 21 de 64 bits. O comando `java -version` deve indicar a versão 21.
3. Git, caso o projeto seja obtido pelo repositório.
4. O Maven Wrapper já está incluído, portanto uma instalação separada do Maven
   não é obrigatória. Se preferir Maven global, use uma versão 3.9 ou superior.

Configure o JDK nesta sessão do PowerShell, se necessário:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

## Compilar, testar e executar

Na pasta do projeto:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
.\mvnw.cmd javafx:run
```

No Linux ou macOS, os comandos equivalentes usam `./mvnw`. A aplicação desta
versão, porém, foi preparada e validada para operação no Windows.

### IntelliJ IDEA

1. Abra a pasta pelo arquivo `pom.xml` e aguarde a importação Maven.
2. Selecione um JDK 21 em **Project Structure > Project SDK**.
3. Execute a configuração Maven `javafx:run`.

Se aparecer “The JavaFX runtime is not configured”, não execute `App.java` como
uma aplicação Java simples. Reimporte o Maven e use `javafx:run`, pois o Maven
fornece os módulos JavaFX necessários.

## Primeiro acesso

Quando não existem usuários, o sistema cria estas credenciais temporárias:

```text
Usuário: admin
Senha: admin
```

A troca por uma senha de pelo menos oito caracteres é obrigatória antes de
entrar no sistema. Enquanto essa troca não tiver sido concluída, a credencial
temporária também é reaplicada a um administrador inicial criado por uma versão
anterior.

## Usuários e permissões

- **Administrador:** usuários, produtos, preços, estoque, vendas, caixa,
  relatórios, configurações e backup.
- **Gerente:** produtos, estoque, descontos, cancelamentos, caixa e relatórios.
- **Operador:** próprio caixa, vendas, recebimentos, fechamento e segunda via.

## Dados da aplicação

Os arquivos ficam fora do diretório de instalação, em:

```text
%LOCALAPPDATA%\PDV Toninho\
├── data\pdv.db
├── backups\
└── logs\
```

Esse local evita problemas de permissão em `Program Files`. Em uma execução de
desenvolvimento especial, a pasta-base pode ser substituída adicionando
`-Dpdv.home=D:\PDV-Dados` às opções da JVM.

O banco, backups e logs são ignorados pelo Git. Se uma versão antiga tiver
dados em `data\pdv.db` dentro do projeto, crie um backup pela versão antiga ou
copie o arquivo com o sistema fechado para a nova pasta `data`.

## Backup e restauração

Na aba **Backup**, o Administrador pode criar uma cópia ou restaurar um arquivo
`.db`. Antes de restaurar, o sistema valida a integridade e cria uma cópia do
banco atual. Reinicie a aplicação depois da restauração. Uma cópia automática
também é criada durante o encerramento normal.

Mantenha cópias periódicas da pasta `backups` em outra unidade. Backup no mesmo
disco não protege contra falha física ou perda do computador.

## Impressora

Defina a impressora desejada como padrão no Windows. O sistema envia o texto do
comprovante para essa impressora. Antes do envio é possível visualizar o
conteúdo. Se a impressora estiver ausente ou falhar, a venda permanece salva e
pode ser reimpressa pelo histórico.

A visualização mantém os acentos normalmente. No envio físico, o sistema
converte o comprovante para caracteres ASCII e troca o espaço especial do
formato `R$` por um espaço comum. Isso evita símbolos corrompidos em impressoras
térmicas que interpretam texto bruto com páginas de código antigas.

## Gerar o aplicativo ou instalador Windows

O JDK 21 inclui `jpackage`. Para criar uma pasta portátil contendo
`PDV Toninho.exe` e um runtime Java próprio:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-windows.ps1
```

Resultado:

```text
target\distribution\PDV Toninho\PDV Toninho.exe
```

Para gerar um instalador `.exe`, instale o WiX Toolset 3.11, deixe suas
ferramentas no `PATH` e execute:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-windows.ps1 -Installer
```

O instalador será gravado em `target\distribution`. O script sempre compila e
executa todos os testes antes de empacotar.

O aplicativo portátil foi gerado e inspecionado com sucesso no ambiente de
desenvolvimento. A variante de instalador não foi executada nesse ambiente
porque o WiX não estava instalado; o próprio script interrompe a geração com
erro caso essa dependência esteja ausente.

## Estrutura do projeto

```text
src/main/java/br/com/loja/pdv/
├── config
├── controller
├── domain
├── exception
├── infrastructure
├── repository
├── service
└── util
```

Os FXML, CSS e scripts de migração ficam em
`src/main/resources/br/com/loja/pdv`. Os testes unitários, de persistência,
FXML e de fluxo completo ficam em `src/test/java/br/com/loja/pdv`.

### Guia para estudar o projeto

O arquivo [`docs/GUIA_DO_CODIGO.md`](docs/GUIA_DO_CODIGO.md) explica a arquitetura, os principais fluxos, as transações e uma ordem recomendada de leitura.

### Como o código está documentado

Cada pacote possui um `package-info.java` explicando sua responsabilidade.
Classes, construtores e métodos relevantes possuem documentação didática. Os
comentários internos mostram etapas de transações, validações, segurança,
estoque, caixa, migrações e restauração de backup. Os testes descrevem o cenário
verificado, e os arquivos FXML, CSS, SQL, Maven e PowerShell identificam o
propósito de suas seções. Para manter compatibilidade, comentários de código
usam apenas caracteres sem acentos; textos da interface continuam acentuados.

## Interface redesenhada

A interface usa navegação lateral, cartões por tarefa, hierarquia clara de ações
e áreas separadas para operações destrutivas. As decisões de usabilidade e as
heurísticas aplicadas estão documentadas em `docs/REDESIGN_INTERFACE.md`.

## Solução de problemas

- **JavaFX não configurado:** confirme JDK 21, reimporte `pom.xml` e execute
  `.\mvnw.cmd javafx:run`.
- **Banco não abre:** confirme permissão de escrita em `%LOCALAPPDATA%` e
  consulte `logs\pdv-0.log`.
- **Login inicial não aparece:** já existe um banco com usuários; restaure o
  backup correto ou use um Administrador existente.
- **Impressão falha:** configure uma impressora padrão e tente a segunda via.
- **Caracteres estranhos no comprovante:** gere novamente o aplicativo com a
  correção de codificação; a impressão física deve exibir `R$ 7,00`, `NAO` e
  `preferencia`, sem sequências de caracteres inválidas.
- **Instalador não é gerado:** confirme `jpackage --version` e `candle -?`.
- **Arquivo de backup recusado:** escolha um banco SQLite íntegro criado pelo
  próprio sistema.

## Limitações da versão

Esta versão não oferece NFC-e, SEFAZ, TEF, PIX automático, integração bancária,
rede com vários caixas, aplicação web ou sincronização em nuvem. A operação é
local e destinada a um computador Windows.
