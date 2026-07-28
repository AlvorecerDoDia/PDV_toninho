package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.ItemVenda;
import br.com.loja.pdv.domain.model.Venda;

import java.util.List;
import java.util.Optional;

public interface VendaRepository {
    Venda finalizar(Venda venda);
    Optional<Venda> buscarPorId(long id);
    List<ItemVenda> listarItens(long vendaId);
}
