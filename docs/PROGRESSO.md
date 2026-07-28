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

## Próxima funcionalidade

Implementar o controle e histórico de estoque.

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
