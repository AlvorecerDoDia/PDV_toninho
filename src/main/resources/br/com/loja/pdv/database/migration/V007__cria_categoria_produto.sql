-- Cria categorias e associa cada produto a exatamente uma categoria.
CREATE TABLE categoria (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL COLLATE NOCASE UNIQUE,
    ativa INTEGER NOT NULL DEFAULT 1
        CHECK (ativa IN (0, 1)),
    criado_em TEXT NOT NULL,
    atualizado_em TEXT NOT NULL
);

INSERT INTO categoria (nome, ativa, criado_em, atualizado_em)
VALUES
    ('Sem categoria', 1, strftime('%Y-%m-%dT%H:%M:%f', 'now'), strftime('%Y-%m-%dT%H:%M:%f', 'now')),
    ('Papelaria', 1, strftime('%Y-%m-%dT%H:%M:%f', 'now'), strftime('%Y-%m-%dT%H:%M:%f', 'now')),
    ('Brinquedos', 1, strftime('%Y-%m-%dT%H:%M:%f', 'now'), strftime('%Y-%m-%dT%H:%M:%f', 'now')),
    ('Acessórios', 1, strftime('%Y-%m-%dT%H:%M:%f', 'now'), strftime('%Y-%m-%dT%H:%M:%f', 'now')),
    ('Outros', 1, strftime('%Y-%m-%dT%H:%M:%f', 'now'), strftime('%Y-%m-%dT%H:%M:%f', 'now'));

ALTER TABLE produto
ADD COLUMN categoria_id INTEGER REFERENCES categoria(id);

UPDATE produto
SET categoria_id = (
    SELECT id FROM categoria WHERE nome = 'Sem categoria'
)
WHERE categoria_id IS NULL;

CREATE INDEX idx_produto_categoria ON produto (categoria_id);
