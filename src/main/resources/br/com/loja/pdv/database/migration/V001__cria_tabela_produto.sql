-- Estrutura principal do catalogo e controle do saldo atual.
-- Cada migracao e executada uma unica vez e registrada em schema_version.
CREATE TABLE IF NOT EXISTS produto (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_barras TEXT UNIQUE,
    nome TEXT NOT NULL,
    preco_custo_centavos INTEGER NOT NULL
        CHECK (preco_custo_centavos >= 0),
    preco_venda_centavos INTEGER NOT NULL
        CHECK (preco_venda_centavos >= 0),
    quantidade_estoque INTEGER NOT NULL DEFAULT 0
        CHECK (quantidade_estoque >= 0),
    estoque_minimo INTEGER NOT NULL DEFAULT 0
        CHECK (estoque_minimo >= 0),
    ativo INTEGER NOT NULL DEFAULT 1
        CHECK (ativo IN (0, 1)),
    criado_em TEXT NOT NULL,
    atualizado_em TEXT NOT NULL
);
