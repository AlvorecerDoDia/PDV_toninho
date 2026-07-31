package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.ItemVenda;
import br.com.loja.pdv.domain.model.Venda;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

/** Contrato transacional de finalizacao, consulta e cancelamento de vendas. */
public interface VendaRepository {
    Venda finalizar(Venda venda);
    Optional<Venda> buscarPorId(long id);
    Optional<Venda> buscarPorNumero(String numero);
    List<Venda> listar(LocalDateTime inicio, LocalDateTime fim, Long operadorId);
    List<ItemVenda> listarItens(long vendaId);
    Venda cancelar(long vendaId, long usuarioId, String motivo, LocalDateTime canceladoEm);
}
