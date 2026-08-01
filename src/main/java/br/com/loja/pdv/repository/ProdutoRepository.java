package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Produto;

import java.util.List;
import java.util.Optional;

/** Contrato CRUD e de pesquisa do catalogo de produtos. */
public interface ProdutoRepository {

    /** Insere um produto. */
    Produto salvar(Produto produto);

    /** Atualiza os dados cadastrais. */
    void atualizar(Produto produto);

    /** Consulta por identificador. */
    Optional<Produto> buscarPorId(long id);

    /** Consulta por codigo de barras. */
    Optional<Produto> buscarPorCodigoBarras(String codigo);

    /** Lista produtos ativos. */
    List<Produto> listarAtivos();

    /** Pesquisa por nome ou codigo. */
    List<Produto> pesquisar(String termo);

    /** Marca o produto como inativo. */
    void desativar(long id);

    /** Marca o produto como ativo. */
    void reativar(long id);
}
