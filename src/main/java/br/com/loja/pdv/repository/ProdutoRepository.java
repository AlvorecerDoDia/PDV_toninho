package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Produto;

import java.util.List;
import java.util.Optional;

/** Contrato CRUD e de pesquisa do catálogo de produtos. */
public interface ProdutoRepository {

    Produto salvar(Produto produto);

    void atualizar(Produto produto);

    Optional<Produto> buscarPorId(long id);

    Optional<Produto> buscarPorCodigoBarras(String codigo);

    List<Produto> listarAtivos();

    List<Produto> pesquisar(String termo);

    void desativar(long id);

    void reativar(long id);
}
