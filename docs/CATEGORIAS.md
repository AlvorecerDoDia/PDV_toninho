# Categorias de produtos

A implementação usa uma entidade persistida porque as categorias são dados da loja,
não regras fixas do programa. Isso evita recompilar o aplicativo sempre que a lista
precisar crescer.

## Estrutura

- `Categoria`: modelo com ID, nome, situação e datas;
- `CategoriaRepository`: contrato de persistência;
- `SQLiteCategoriaRepository`: implementação SQLite;
- `CategoriaService`: validação, cadastro, consulta, ativação e desativação;
- `produto.categoria_id`: chave estrangeira que liga cada produto a uma categoria;
- `ComboBox<Categoria>`: seleção obrigatória no cadastro de produtos.

## Migração de bancos existentes

A migração `V007__cria_categoria_produto.sql` cria as categorias iniciais e atribui
`Sem categoria` aos produtos já existentes. Assim, nenhum registro antigo é perdido.

## Categorias iniciais

- Sem categoria
- Papelaria
- Brinquedos
- Acessórios
- Outros

A estrutura já permite adicionar uma tela de administração de categorias no futuro.
