package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.MovimentacaoEstoque;

import java.time.LocalDateTime;
import java.util.List;

/** Contrato de alteracao atomica e consulta do estoque. */
public interface EstoqueRepository {
    /** Altera saldo e persiste o historico na mesma transacao. */
    MovimentacaoEstoque registrar(MovimentacaoEstoque movimentacao);
    /** Consulta o saldo atual de um produto. */
    int buscarSaldo(long produtoId);
    /** Lista movimentacoes por produto e periodo. */
    List<MovimentacaoEstoque> listar(long produtoId, LocalDateTime inicio, LocalDateTime fim);
}
