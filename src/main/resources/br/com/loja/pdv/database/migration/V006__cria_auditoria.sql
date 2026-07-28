-- Registro das ações administrativas e alterações sensíveis.
CREATE TABLE auditoria (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER,
    acao TEXT NOT NULL,
    entidade TEXT NOT NULL,
    entidade_id INTEGER,
    valores_anteriores TEXT,
    valores_novos TEXT,
    criado_em TEXT NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE INDEX idx_auditoria_entidade
    ON auditoria (entidade, entidade_id, criado_em);
CREATE INDEX idx_auditoria_usuario
    ON auditoria (usuario_id, criado_em);
