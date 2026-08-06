package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Categoria;

import java.util.List;
import java.util.Optional;

/** Contrato simples de cadastro e consulta de categorias. */
public interface CategoriaRepository {
    Categoria salvar(Categoria categoria);
    void atualizar(Categoria categoria);
    Optional<Categoria> buscarPorId(long id);
    Optional<Categoria> buscarPorNome(String nome);
    List<Categoria> listarTodas();
}
