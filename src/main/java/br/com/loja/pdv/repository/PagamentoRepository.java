package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Pagamento;

import java.util.List;

public interface PagamentoRepository {
    List<Pagamento> listarPorVenda(long vendaId);
}
