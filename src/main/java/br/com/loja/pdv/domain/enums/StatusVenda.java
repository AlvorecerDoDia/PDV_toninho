package br.com.loja.pdv.domain.enums;

/** Estados persistidos de uma venda. */
public enum StatusVenda {
    FINALIZADA, // Venda persistida e contabilizada.
    CANCELADA   // Venda estornada, mas preservada no historico.
}
