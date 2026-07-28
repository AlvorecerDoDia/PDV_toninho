package br.com.loja.pdv.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/** Perfis que agrupam as permissões disponíveis no sistema. */
public enum PerfilUsuario {
    ADMINISTRADOR(EnumSet.allOf(Permissao.class)),
    GERENTE(EnumSet.of(
            Permissao.PRODUTOS, Permissao.PRECOS, Permissao.ESTOQUE,
            Permissao.VENDAS, Permissao.CAIXA, Permissao.RELATORIOS,
            Permissao.DESCONTOS, Permissao.CANCELAMENTOS, Permissao.REIMPRESSAO
    )),
    OPERADOR(EnumSet.of(
            Permissao.VENDAS, Permissao.CAIXA,
            Permissao.REIMPRESSAO, Permissao.FECHAR_PROPRIO_CAIXA
    ));

    private final Set<Permissao> permissions;

    PerfilUsuario(Set<Permissao> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

    public boolean permite(Permissao permissao) {
        return permissions.contains(permissao);
    }
}
