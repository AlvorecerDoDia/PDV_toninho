package br.com.loja.pdv.infrastructure.printing;

import br.com.loja.pdv.domain.model.Venda;

/** Porta de impressão que permite trocar ou simular a impressora física. */
public interface ImpressoraComprovante {
    void imprimir(Venda venda, boolean segundaVia);
}
