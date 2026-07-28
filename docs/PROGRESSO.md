# Progresso do PDV Toninho

## Fundação e configuração do projeto

- Funcionalidade: auditoria e correção da estrutura inicial.
- Resultado: pacotes protótipos removidos, aplicação consolidada em `br.com.loja.pdv`, recursos organizados em `view/`, interface de repositório movida para a camada correta e arquivos locais protegidos pelo `.gitignore`.
- Testes executados antes da correção: `mvn clean test` e `mvn clean package`.
- Resultado antes da correção: sucesso, porém sem testes automatizados.
- Testes executados após a correção: `mvn clean test` e `mvn clean package`.
- Resultado após a correção: ambos concluídos com `BUILD SUCCESS`; a base ainda não possuía testes automatizados.
- Teste manual: carregamento do FXML principal será validado na etapa de cadastro de produtos.
- Commit: `chore: corrige estrutura inicial e configuração do projeto`.
- Pendências reais: migrações versionadas, implementação do repositório SQLite e CRUD completo de produtos.

## Próxima funcionalidade

Implementar migrações versionadas e testáveis do banco SQLite.
