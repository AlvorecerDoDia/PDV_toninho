# Progresso do PDV Toninho

## Fundação e configuração do projeto

- Funcionalidade: auditoria e correção da estrutura inicial.
- Resultado: pacotes protótipos removidos, aplicação consolidada em `br.com.loja.pdv`, recursos organizados em `view/`, interface de repositório movida para a camada correta e arquivos locais protegidos pelo `.gitignore`.
- Testes executados antes da correção: `mvn clean test` e `mvn clean package`.
- Resultado antes da correção: sucesso, porém sem testes automatizados.
- Testes executados após a correção: `mvn clean test` e `mvn clean package`.
- Resultado após a correção: ambos concluídos com `BUILD SUCCESS`; a base ainda não possuía testes automatizados.
- Teste manual: carregamento e uso do FXML principal validados na etapa de cadastro de produtos.
- Commit: `chore: corrige estrutura inicial e configuração do projeto`.
- Pendências reais: migrações versionadas, implementação do repositório SQLite e CRUD completo de produtos.

## Migrações versionadas do banco

- Funcionalidade: inicialização e migração transacional do SQLite.
- Resultado: tabela `schema_version`, script `V001__cria_tabela_produto.sql`, execução única por versão, rollback em falha e rejeição de versões desconhecidas.
- Configuração da conexão: chaves estrangeiras ativadas, espera de 5 segundos e modo WAL.
- Testes criados: banco vazio, reinicialização idempotente, restrições `CHECK`, chaves estrangeiras, WAL, diretório inexistente, erro de diretório e versão desconhecida.
- Testes executados: `mvn clean test` e `mvn clean package`.
- Resultado: 7 testes aprovados, sem falhas ou erros; ambos os comandos concluíram com `BUILD SUCCESS`.
- Teste manual: não utiliza o banco real; todos os cenários usam diretórios temporários isolados.
- Commit: `feat: implementa migrações versionadas do banco`.
- Pendências reais: CRUD completo e interface de cadastro de produtos.

## Cadastro completo de produtos

- Funcionalidade: CRUD de produtos com tela JavaFX.
- Resultado: cadastro, atualização, pesquisa, desativação, reativação e persistência SQLite implementados.
- Validações: nome obrigatório e normalizado, valores monetários não negativos, duas casas decimais, estoque mínimo não negativo e código de barras opcional e único.
- Segurança dos dados: dinheiro persistido em centavos; quantidade de estoque não pode ser editada pelo cadastro.
- Testes criados: serviço, repositório SQLite com banco temporário e carregamento completo do FXML.
- Testes executados: `mvn clean test` e `mvn clean package`.
- Resultado: 19 testes aprovados, sem falhas ou erros; ambos os comandos concluíram com `BUILD SUCCESS`.
- Teste manual: aprovado pelo usuário em 28/07/2026; tela e operações visuais funcionando corretamente.
- Commit: `feat: implementa cadastro completo de produtos`.
- Pendências reais: controle transacional e histórico de estoque.

## Controle e histórico de estoque

- Funcionalidade: entradas, ajustes, perdas, devoluções, saldo e histórico por produto e período.
- Resultado: saldo e movimentação são gravados na mesma transação; qualquer falha executa rollback integral.
- Regras: quantidade positiva, saldo nunca negativo, motivo obrigatório em ajustes e perdas, produto existente e ativo e saída de venda bloqueada fora da finalização.
- Interface: aba de estoque com produto, saldo, tipo, quantidade, motivo, filtros por período e histórico; saldo abaixo do mínimo recebe destaque.
- Migração: `V002__cria_movimentacao_estoque.sql`.
- Testes criados: serviço, repositório SQLite, histórico, produto inativo, estoque negativo e rollback provocado.
- Testes executados: `mvn clean test` e `mvn clean package`.
- Resultado: 30 testes aprovados, sem falhas ou erros; ambos os comandos concluíram com `BUILD SUCCESS`.
- Teste de integração: FXML principal, produtos e estoque carregados em conjunto sem erro.
- Commit: `feat: implementa controle e histórico de estoque`.
- Pendências reais: usuários, autenticação e permissões.

## Usuários, login e permissões

- Funcionalidade: cadastro e manutenção de usuários, autenticação e autorização por perfil.
- Resultado: login com senha protegida por PBKDF2, sessão explícita, usuários ativos e inativos, troca obrigatória da senha inicial e perfis Administrador, Gerente e Operador.
- Segurança: nenhum valor de senha é persistido em texto puro; o primeiro administrador usa a credencial temporária `admin`/`admin` e precisa substituí-la antes de entrar.
- Permissões: matriz centralizada por perfil e ocultação das áreas não autorizadas da interface.
- Migração: `V003__cria_usuario.sql`.
- Testes criados: criação do administrador inicial, hash de senha, login válido e inválido, usuário inativo, troca de senha, sessão, permissões e carregamento dos FXMLs principal e de login.
- Testes executados: `mvn clean test` e `mvn clean package`.
- Resultado: 37 testes aprovados, sem falhas ou erros; ambos os comandos concluíram com `BUILD SUCCESS`.
- Commit: `feat: implementa usuários login e permissões`.
- Pendências reais: abertura, movimentação, sangria, suprimento e fechamento de caixa.

## Abertura e movimentações de caixa

- Funcionalidade: abertura, suprimento, sangria e fechamento individual de caixa.
- Resultado: cada operador possui no máximo um caixa aberto; abertura e movimentação inicial são atômicas, o caixa fechado rejeita novas movimentações e a sangria nunca supera o dinheiro esperado.
- Fechamento: registra valor esperado, valor contado e diferença; o operador só recebe esses valores depois de informar a contagem.
- Persistência: valores monetários armazenados em centavos e histórico completo de movimentações associado ao usuário.
- Interface: aba de caixa com abertura, suprimento, sangria, fechamento e histórico.
- Migração: `V004__cria_caixa.sql`.
- Testes criados: abertura, duplicidade, suprimento, sangria, saldo insuficiente, motivo, precisão monetária, fechamento, diferença, usuário, persistência, caixa fechado e rollback provocado.
- Testes executados: `mvn clean test` e `mvn clean package`.
- Resultado: 47 testes aprovados, sem falhas ou erros; ambos os comandos concluíram com `BUILD SUCCESS`.
- Teste de integração: FXML principal carregado com a nova aba de caixa.
- Commit: `feat: implementa abertura e movimentações de caixa`.
- Pendências reais: carrinho e tela de venda.

## Carrinho e tela de venda

- Funcionalidade: montagem do carrinho em memória e operação da tela de venda.
- Resultado: inclusão por código de barras ou pesquisa, acúmulo de produto repetido, alteração de quantidade, remoção, limpeza, subtotal, desconto autorizado e total.
- Regras: produto inexistente ou inativo é recusado, quantidades precisam ser positivas, o estoque disponível é respeitado e o desconto não pode ser negativo nem superar o subtotal.
- Permissões: acesso exige permissão de vendas e aplicação de desconto exige autorização específica.
- Interface: aba de venda com foco inicial no código de barras e atalhos F2, F4, Delete, F6 e Esc.
- Testes criados: produto adicionado e repetido, quantidade, remoção, limpeza, subtotal, desconto, produto inexistente, produto inativo, estoque insuficiente e carrinho vazio.
- Testes executados: `mvn clean test` e `mvn clean package`.
- Resultado: 58 testes aprovados, sem falhas ou erros; ambos os comandos concluíram com `BUILD SUCCESS`.
- Teste de integração: FXML principal carregado com a tela de venda e todos os controllers.
- Commit: `feat: implementa carrinho e tela de venda`.
- Pendências reais: pagamentos e finalização transacional da venda.

## Pagamentos e finalização transacional de vendas

- Funcionalidade: recebimentos em dinheiro, PIX, cartão de débito, cartão de crédito e combinações.
- Resultado: venda numerada com operador e caixa, itens e pagamentos persistidos, custo e preço históricos, troco exclusivo do dinheiro, baixa de estoque e entrada do valor líquido em dinheiro no caixa.
- Transação: valida caixa, carrinho, preços e estoque; grava venda, itens e pagamentos; baixa estoque; cria históricos de estoque e caixa; qualquer falha desfaz toda a operação.
- Regras: venda vazia ou sem caixa aberto é recusada, pagamentos precisam ser positivos e suficientes, PIX e cartão não geram troco e estoque e preço são verificados novamente na finalização.
- Interface: painel de pagamentos integrado à tela de venda, pagamento combinado, valores recebido/restante/troco e atalho F10.
- Migração: `V005__cria_venda.sql`.
- Testes criados: dinheiro e troco, PIX, débito, crédito, combinado, insuficiência, troco inválido, venda vazia, caixa fechado, estoque, persistência, custo histórico, movimentação de caixa e rollback total provocado.
- Testes executados: `mvn clean test` e `mvn clean package`.
- Resultado: 75 testes aprovados, sem falhas ou erros; ambos os comandos concluíram com `BUILD SUCCESS`.
- Teste de integração: FXML principal carregado com carrinho e painel de pagamentos compartilhando o mesmo estado.
- Commit: `feat: implementa finalização transacional de vendas`.
- Pendências reais: histórico e cancelamento transacional de vendas.

## Histórico e cancelamento de vendas

- Funcionalidade: consulta de vendas por número, período e operador, visualização de itens e pagamentos e cancelamento autorizado.
- Resultado: histórico exibe status e valores; o cancelamento exige motivo, só pode ocorrer uma vez e é restrito a perfis autorizados.
- Transação: o cancelamento altera a venda, devolve os produtos ao estoque, cria movimentações de devolução, estorna o dinheiro do caixa e grava auditoria; qualquer falha desfaz tudo.
- Caixa fechado: o estorno permanece registrado e recalcula o valor esperado e a diferença do fechamento original.
- Interface: aba de histórico com filtros, detalhes completos e cancelamento da venda selecionada.
- Migração: `V006__cria_auditoria.sql`, introduzindo a base de auditoria usada pelo cancelamento.
- Testes criados: consultas, filtros, cancelamento, estoque devolvido, estorno de caixa aberto e fechado, duplicidade, permissão, motivo obrigatório, auditoria e rollback provocado.
- Testes executados: `mvn clean test` e `mvn clean package`.
- Resultado: 81 testes aprovados, sem falhas ou erros; ambos os comandos concluíram com `BUILD SUCCESS`.
- Teste de integração: FXML principal carregado com histórico, carrinho e pagamentos.
- Commit: `feat: implementa histórico e cancelamento de vendas`.
- Pendências reais: impressão e segunda via do comprovante não fiscal.

## Impressão e segunda via de comprovantes

- Funcionalidade: formatação, visualização, impressão comum e segunda via.
- Resultado: comprovante textual com loja, indicação `COMPROVANTE NÃO FISCAL`, venda, data, operador, status, itens, preços históricos, totais, desconto, pagamentos, recebido e troco.
- Segunda via: recebe também a identificação `SEGUNDA VIA`.
- Segurança operacional: o conteúdo é exibido para confirmação antes da impressão física; indisponibilidade ou falha da impressora não altera nem perde a venda.
- Integração Windows: envio para a impressora padrão por meio do serviço nativo de impressão do Java.
- Interface: ações de visualizar, imprimir e emitir segunda via no histórico de vendas.
- Testes criados: conteúdo completo, aviso não fiscal, segunda via e uso dos valores históricos.
- Testes executados: `mvn clean test` e `mvn clean package`.
- Resultado: 84 testes aprovados, sem falhas ou erros; ambos os comandos concluíram com `BUILD SUCCESS`.
- Teste físico: não executado automaticamente para evitar dependência de impressora; ausência de impressora retorna mensagem clara e mantém a venda salva.
- Commit: `feat: implementa impressão e segunda via de comprovantes`.
- Pendências reais: relatórios básicos e exportação CSV.

## Relatórios básicos e exportação CSV

- Funcionalidade: vendas por dia, período e operador; pagamentos; produtos mais vendidos; estoque baixo; movimentações; descontos; cancelamentos; fechamentos; lucro bruto estimado.
- Filtros: período, operador, forma de pagamento e produto.
- Regras: vendas canceladas ficam fora dos totais normais e o lucro usa custo e preço históricos.
- Interface: aba de relatórios com filtros, tabela e exportação CSV.
- Exportação: UTF-8, valores no padrão brasileiro, campos escapados e proteção contra fórmulas de planilha.
- Testes criados: consultas com banco conhecido, filtros, somatórios, cancelamentos, lucro histórico, fechamento, estoque baixo e CSV.
- Testes executados: `mvn clean test` e `mvn clean package`.
- Resultado: 88 testes aprovados, sem falhas ou erros; ambos os comandos concluíram com `BUILD SUCCESS`.
- Commit: `feat: implementa relatórios básicos do pdv`.
- Pendências reais: backup e restauração segura.

## Backup e restauração do banco

- Funcionalidade: criação manual e automática de cópias consistentes do banco SQLite, listagem, retenção e restauração segura.
- Segurança: validação de integridade antes da restauração, cópia preventiva do banco atual, arquivo temporário e substituição atômica quando suportada.
- Recuperação: remoção dos arquivos auxiliares WAL/SHM e reaplicação das migrações após restaurar uma cópia válida.
- Interface: aba administrativa para criar, atualizar, restaurar e abrir a pasta de backups.
- Automação: cópia realizada ao encerrar normalmente a aplicação.
- Testes criados: criação consistente, restauração, cópia preventiva, rejeição de arquivo inválido, retenção e diretório inacessível.
- Testes executados: `mvn clean test`.
- Resultado: 93 testes aprovados, sem falhas ou erros.
- Commit: `feat: implementa backup e restauração do banco`.
- Pendências reais: auditoria completa, logs e tratamento centralizado de erros.

## Auditoria, logs e tratamento de erros

- Auditoria: repositório SQLite e serviço para consultar e registrar usuário, ação, entidade, valores anteriores, valores novos e data.
- Operações auditadas: alteração de preço, ajuste de estoque, sangria, suprimento, desconto, cancelamento, criação/alteração de usuário, troca de senha sem registrar credenciais e restauração de backup.
- Transações: desconto e cancelamento são auditados dentro da mesma transação da venda.
- Logs: arquivos rotativos em `logs/`, com limite de tamanho, retenção e registro global de erros não tratados.
- Tratamento centralizado: validação, banco, impressão, formato numérico e falhas inesperadas recebem mensagens apropriadas sem expor detalhes técnicos.
- Segurança: senhas, hashes e dados de cartão não são incluídos na auditoria ou nas mensagens.
- Testes criados: persistência da auditoria, operações auditáveis, desconto transacional e tradução centralizada de erros.
- Testes executados: `mvn clean test`.
- Resultado: 100 testes aprovados, sem falhas ou erros.
- Commit: `feat: adiciona auditoria logs e tratamento de erros`.
- Pendências reais: padronização visual e revisão de usabilidade.

## Interface e usabilidade

- Identidade visual: CSS centralizado com cores, tipografia, botões, campos, abas e tabelas consistentes.
- Navegação: barra principal com nome do sistema, usuário/perfil atual e indicador de caixa aberto ou fechado.
- Operação: tamanhos adequados para 1366×768, foco e atalhos da venda preservados e campos monetários/inteiros com formatação e restrição de entrada.
- Segurança: confirmações para desativação de produto, sangria, fechamento de caixa, cancelamento de venda e restauração.
- Legibilidade: títulos padronizados, contraste, linhas alternadas, seleção visível e ações principais/perigosas diferenciadas.
- Testes atualizados: carregamento do login e de todos os FXML, associação do CSS e validação do layout em 1366×768.
- Testes executados: testes de FXML específicos e suíte completa.
- Commit: `style: padroniza interface e usabilidade do sistema`.
- Pendências reais: teste integrado do fluxo completo.

## Teste integrado completo

- Fluxo automatizado: banco vazio, administrador, login, produto, entrada de estoque, abertura de caixa, venda com dinheiro e PIX, desconto, troco, baixa de estoque e entrada no caixa.
- Continuação do fluxo: comprovante não fiscal, histórico, fechamento sem diferença, relatório, backup, alteração posterior e restauração do estado salvo.
- Falhas cobertas pela suíte: código duplicado, estoque negativo, venda sem caixa, pagamento insuficiente, cancelamento sem permissão, rollback no meio da transação, impressora indisponível e restauração inválida.
- Isolamento: todos os testes usam diretórios e bancos temporários; o banco real não é acessado.
- Testes executados: `mvn clean test` e `mvn clean package`.
- Resultado: 102 testes aprovados, sem falhas ou erros.
- Commit: `test: adiciona testes integrados do fluxo completo do pdv`.
- Pendências reais: empacotamento Windows e documentação final.

## Empacotamento e documentação

- Dados permanentes: banco, backups e logs ficam em `%LOCALAPPDATA%\PDV Toninho`, fora de `Program Files` e do repositório.
- Empacotamento: script PowerShell com Maven e `jpackage` para aplicativo portátil com runtime próprio e opção de instalador `.exe`.
- Validação: aplicativo portátil gerado com `PDV Toninho.exe`, runtime Java, JavaFX, SQLite e classpath conferidos; o executável foi iniciado em um diretório temporário e permaneceu em execução com banco e logs criados corretamente.
- Instalador: opção documentada e preparada; a geração `.exe` exige WiX Toolset 3.11, ausente no ambiente atual.
- Documentação: objetivo, funções, tecnologias, JDK/Maven, compilação, execução, IntelliJ/JavaFX, primeiro acesso, permissões, dados, backup, impressora, empacotamento, limitações e solução de problemas.
- Testes executados: `mvn clean test`, `mvn clean package` e `scripts/build-windows.ps1`.
- Resultado: 105 testes aprovados, sem falhas ou erros; pacote portátil criado com sucesso.
- Commit: `build: prepara distribuição e documentação do pdv`.
- Pendências impeditivas: nenhuma.

## Situação final

Primeira versão funcional concluída e pronta para uso local em um computador Windows.

## Ajuste de credencial inicial

- Primeiro acesso configurado como usuário `admin` e senha temporária `admin`.
- A senha continua armazenada somente como hash e a troca por uma senha forte permanece obrigatória.
- Administradores iniciais ainda pendentes de troca recebem a credencial temporária atualizada; senhas já alteradas não são sobrescritas.
- Validação: 105 testes aprovados e novo executável iniciado com sucesso usando um banco temporário.
