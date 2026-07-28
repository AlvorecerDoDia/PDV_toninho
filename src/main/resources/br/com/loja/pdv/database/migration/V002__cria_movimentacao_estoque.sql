CREATE TABLE movimentacao_estoque (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    tipo TEXT NOT NULL CHECK (tipo IN (
        'ENTRADA', 'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO',
        'SAIDA_VENDA', 'DEVOLUCAO', 'PERDA'
    )),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    quantidade_anterior INTEGER NOT NULL CHECK (quantidade_anterior >= 0),
    quantidade_posterior INTEGER NOT NULL CHECK (quantidade_posterior >= 0),
    motivo TEXT,
    usuario_id INTEGER,
    venda_id INTEGER,
    criado_em TEXT NOT NULL,
    FOREIGN KEY (produto_id) REFERENCES produto(id)
);

CREATE INDEX idx_movimentacao_estoque_produto_data
    ON movimentacao_estoque (produto_id, criado_em);
