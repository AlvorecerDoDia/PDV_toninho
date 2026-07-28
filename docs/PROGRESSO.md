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
- Segurança: nenhum valor de senha é persistido em texto puro; o primeiro administrador usa senha temporária aleatória ou a variável `PDV_ADMIN_PASSWORD`, exibida uma única vez.
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

## Próxima funcionalidade

Implementar impressão e segunda via de comprovantes.
