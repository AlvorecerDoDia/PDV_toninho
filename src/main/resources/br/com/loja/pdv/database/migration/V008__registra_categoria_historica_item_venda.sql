-- Guarda a categoria do produto no momento em que cada item e vendido.
ALTER TABLE item_venda
ADD COLUMN categoria_id INTEGER REFERENCES categoria(id);

ALTER TABLE item_venda
ADD COLUMN categoria_nome TEXT;

-- Preenche vendas antigas usando a categoria atual do produto.
UPDATE item_venda
SET categoria_id = (
        SELECT p.categoria_id
        FROM produto p
        WHERE p.id = item_venda.produto_id
    ),
    categoria_nome = COALESCE((
        SELECT c.nome
        FROM produto p
        LEFT JOIN categoria c ON c.id = p.categoria_id
        WHERE p.id = item_venda.produto_id
    ), 'Sem categoria')
WHERE categoria_nome IS NULL;

CREATE INDEX idx_item_venda_categoria
ON item_venda (categoria_id);
