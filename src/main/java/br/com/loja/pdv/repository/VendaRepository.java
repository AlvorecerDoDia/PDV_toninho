package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.ItemVenda;
import br.com.loja.pdv.domain.model.ProdutoVendidoHistorico;
import br.com.loja.pdv.domain.model.Venda;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDateTime;

/** Contrato transacional de finalizacao, consulta e cancelamento de vendas. */
public interface VendaRepository {
    /** Persiste a venda e todos os efeitos relacionados. */
    Venda finalizar(Venda venda);
    /** Consulta uma venda por identificador. */
    Optional<Venda> buscarPorId(long id);
    /** Consulta uma venda pelo numero publico. */
    Optional<Venda> buscarPorNumero(String numero);
    /** Pesquisa vendas por filtros opcionais. */
    List<Venda> listar(LocalDateTime inicio, LocalDateTime fim, Long operadorId);
    /** Lista os itens de uma venda. */
    List<ItemVenda> listarItens(long vendaId);
    /** Lista produtos vendidos no periodo e permite filtrar varias categorias. */
    List<ProdutoVendidoHistorico> listarProdutosVendidos(
            LocalDateTime inicio, LocalDateTime fim, Set<Long> categoriaIds);
    /** Cancela e estorna uma venda de forma atomica. */
    Venda cancelar(long vendaId, long usuarioId, String motivo, LocalDateTime canceladoEm);
}
