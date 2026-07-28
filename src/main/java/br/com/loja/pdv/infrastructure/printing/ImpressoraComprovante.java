package br.com.loja.pdv.infrastructure.printing;

import br.com.loja.pdv.domain.model.Venda;

public interface ImpressoraComprovante {
    void imprimir(Venda venda, boolean segundaVia);
}
