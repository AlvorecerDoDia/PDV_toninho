package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Produto;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository {

    Produto salvar(Produto produto);

    void atualizar(Produto produto);

    Optional<Produto> buscarPorId(long id);

    Optional<Produto> buscarPorCodigoBarras(String codigo);

    List<Produto> listarAtivos();

    void desativar(long id);
}
