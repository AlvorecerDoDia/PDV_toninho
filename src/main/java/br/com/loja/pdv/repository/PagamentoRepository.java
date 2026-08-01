package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Pagamento;

import java.util.List;

/** Contrato de consulta dos pagamentos persistidos por venda. */
public interface PagamentoRepository {
    /** Lista as parcelas de pagamento de uma venda. */
    List<Pagamento> listarPorVenda(long vendaId);
}
