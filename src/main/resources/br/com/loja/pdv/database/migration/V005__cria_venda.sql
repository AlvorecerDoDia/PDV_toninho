CREATE TABLE venda (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero TEXT NOT NULL UNIQUE,
    operador_id INTEGER NOT NULL,
    caixa_id INTEGER NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('FINALIZADA', 'CANCELADA')),
    subtotal_centavos INTEGER NOT NULL CHECK (subtotal_centavos >= 0),
    desconto_centavos INTEGER NOT NULL CHECK (desconto_centavos >= 0),
    total_centavos INTEGER NOT NULL CHECK (total_centavos >= 0),
    troco_centavos INTEGER NOT NULL CHECK (troco_centavos >= 0),
    criado_em TEXT NOT NULL,
    cancelado_em TEXT,
    motivo_cancelamento TEXT,
    FOREIGN KEY (operador_id) REFERENCES usuario(id),
    FOREIGN KEY (caixa_id) REFERENCES caixa(id)
);

CREATE TABLE item_venda (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    venda_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
    produto_nome TEXT NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    custo_unitario_centavos INTEGER NOT NULL CHECK (custo_unitario_centavos >= 0),
    preco_unitario_centavos INTEGER NOT NULL CHECK (preco_unitario_centavos >= 0),
    subtotal_centavos INTEGER NOT NULL CHECK (subtotal_centavos >= 0),
    FOREIGN KEY (venda_id) REFERENCES venda(id),
    FOREIGN KEY (produto_id) REFERENCES produto(id)
);

CREATE TABLE pagamento (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    venda_id INTEGER NOT NULL,
    forma TEXT NOT NULL CHECK (forma IN (
        'DINHEIRO', 'PIX', 'CARTAO_DEBITO', 'CARTAO_CREDITO'
    )),
    valor_centavos INTEGER NOT NULL CHECK (valor_centavos > 0),
    criado_em TEXT NOT NULL,
    FOREIGN KEY (venda_id) REFERENCES venda(id)
);

CREATE INDEX idx_venda_data ON venda (criado_em);
CREATE INDEX idx_venda_operador ON venda (operador_id, criado_em);
CREATE INDEX idx_item_venda_venda ON item_venda (venda_id);
CREATE INDEX idx_pagamento_venda ON pagamento (venda_id);
