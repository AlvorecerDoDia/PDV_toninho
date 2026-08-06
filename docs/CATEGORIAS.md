# Categorias de produtos

Categorias sao dados persistidos no SQLite. Cada produto pertence a uma categoria.

A tela Cadastro possui uma aba Categorias para:

- cadastrar;
- renomear;
- listar.

Categorias nao sao apagadas porque produtos e vendas antigas podem referencia-las.
A migracao V007 cria as categorias iniciais e associa produtos antigos a
`Sem categoria`.

O Historico usa a categoria gravada no item no momento da venda. Por isso, o nome
historico permanece correto mesmo quando a categoria atual e renomeada.
