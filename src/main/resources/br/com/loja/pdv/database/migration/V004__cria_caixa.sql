-- Sessões de caixa e lançamentos financeiros vinculados ao operador.
CREATE TABLE caixa (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('ABERTO', 'FECHADO')),
    valor_abertura_centavos INTEGER NOT NULL CHECK (valor_abertura_centavos >= 0),
    valor_esperado_centavos INTEGER,
    valor_contado_centavos INTEGER,
    diferenca_centavos INTEGER,
    aberto_em TEXT NOT NULL,
    fechado_em TEXT,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE UNIQUE INDEX idx_caixa_usuario_aberto
    ON caixa (usuario_id) WHERE status = 'ABERTO';

CREATE TABLE movimentacao_caixa (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    caixa_id INTEGER NOT NULL,
    usuario_id INTEGER NOT NULL,
    tipo TEXT NOT NULL CHECK (tipo IN (
        'ABERTURA', 'VENDA_DINHEIRO', 'SUPRIMENTO', 'SANGRIA', 'ESTORNO'
    )),
    valor_centavos INTEGER NOT NULL CHECK (valor_centavos >= 0),
    motivo TEXT,
    criado_em TEXT NOT NULL,
    FOREIGN KEY (caixa_id) REFERENCES caixa(id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE INDEX idx_movimentacao_caixa_caixa_data
    ON movimentacao_caixa (caixa_id, criado_em);
