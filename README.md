# PDV Toninho

Sistema de ponto de venda local para Windows, criado com Java 21, JavaFX e SQLite.
A versao simplificada concentra o projeto no fluxo principal da loja: cadastrar,
controlar estoque, vender, imprimir e consultar o historico.

> O comprovante emitido e nao fiscal. O sistema nao emite NFC-e e nao se comunica
> com a SEFAZ.

## Funcionalidades mantidas

- login simples com usuario e senha;
- cadastro e manutencao de usuarios, sem perfis diferentes;
- cadastro de produtos com categoria, precos, estoque inicial e estoque minimo;
- cadastro e renomeacao de categorias;
- entrada de mercadoria e ajuste direto do saldo;
- carrinho de venda, desconto, uma forma de pagamento por venda e troco em dinheiro;
- abertura e fechamento simples do caixa;
- comprovante nao fiscal com visualizacao, impressao, segunda via e corte ESC/POS;
- historico completo de vendas, detalhes, pagamentos, cancelamento e reimpressao;
- historico de produtos vendidos por periodo e por uma ou varias categorias;
- preservacao da categoria historica do produto no momento da venda;
- backup automatico do banco ao encerrar normalmente;
- migracoes automaticas do SQLite e arquivo de log simples.

## Simplificacoes realizadas

Foram retirados os recursos que aumentavam a quantidade de classes e regras sem
serem essenciais ao uso basico:

- perfis e matriz de permissoes;
- troca obrigatoria de senha no primeiro acesso;
- pagamentos combinados;
- suprimento e sangria manuais;
- ativacao e desativacao de categorias;
- relatorios separados e exportacao CSV;
- auditoria administrativa;
- tela de backup e restauracao;
- geracao opcional de instalador com WiX.

O modulo **Historico nao foi reduzido**. Seus filtros por periodo, varias
categorias, categoria historica, cancelamento e reimpressao continuam presentes.

## Primeiro acesso

Quando o banco nao possui usuarios, o sistema cria:

```text
Usuario: admin
Senha: admin
```

A senha pode ser alterada posteriormente na tela de usuarios.

## Dados locais

Por padrao, os dados ficam em:

```text
%LOCALAPPDATA%\PDV Toninho\
├── data\pdv.db
├── backups\
└── logs\pdv.log
```

Durante o desenvolvimento, a pasta pode ser substituida com:

```text
-Dpdv.home=D:\PDV-Dados
```

## Compilar, testar e executar

Na pasta do projeto:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
.\mvnw.cmd javafx:run
```

No IntelliJ IDEA, abra o `pom.xml`, selecione um JDK 21 e execute a configuracao
Maven `javafx:run`.

## Gerar o aplicativo portatil do Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-windows.ps1
```

O resultado fica em:

```text
target\distribution\PDV Toninho\PDV Toninho.exe
```

## Impressora

Defina a impressora termica como padrao no Windows. O envio fisico usa PC860
(`IBM860`) e comandos ESC/POS. Ao final, o sistema envia um comando de corte
parcial. O corte depende de a impressora possuir autocutter compativel.

Uma falha de impressao nao apaga a venda. O comprovante pode ser emitido novamente
pelo Historico.

## Estrutura principal

```text
FXML/CSS -> Controller -> Service -> Repository -> SQLite
```

As migracoes ficam em:

```text
src/main/resources/br/com/loja/pdv/database/migration
```

Nunca altere a numeracao de uma migracao que ja foi executada. Mudancas futuras
devem receber uma nova versao.

## Limites desta versao

O sistema nao oferece NFC-e, SEFAZ, TEF, PIX automatico, integracao bancaria,
multiplos computadores em rede, nuvem ou aplicacao web.
