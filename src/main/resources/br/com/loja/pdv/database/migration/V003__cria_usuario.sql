-- Usuários locais, perfis de acesso e credenciais armazenadas por hash.
CREATE TABLE usuario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    login TEXT NOT NULL COLLATE NOCASE UNIQUE,
    senha_hash TEXT NOT NULL,
    perfil TEXT NOT NULL CHECK (perfil IN ('ADMINISTRADOR', 'GERENTE', 'OPERADOR')),
    ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1)),
    alterar_senha INTEGER NOT NULL DEFAULT 1 CHECK (alterar_senha IN (0, 1)),
    criado_em TEXT NOT NULL,
    atualizado_em TEXT NOT NULL
);
