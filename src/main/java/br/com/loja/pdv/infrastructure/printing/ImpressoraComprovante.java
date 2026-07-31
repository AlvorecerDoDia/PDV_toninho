package br.com.loja.pdv.infrastructure.printing;

import br.com.loja.pdv.domain.model.Venda;

/** Porta de impressao que permite trocar ou simular a impressora fisica. */
public interface ImpressoraComprovante {
    void imprimir(Venda venda, boolean segundaVia);
}
