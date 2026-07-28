package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.MovimentacaoEstoque;

import java.time.LocalDateTime;
import java.util.List;

/** Contrato de alteração atômica e consulta do estoque. */
public interface EstoqueRepository {
    MovimentacaoEstoque registrar(MovimentacaoEstoque movimentacao);
    int buscarSaldo(long produtoId);
    List<MovimentacaoEstoque> listar(long produtoId, LocalDateTime inicio, LocalDateTime fim);
}
