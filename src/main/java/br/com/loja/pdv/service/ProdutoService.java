package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.repository.sqlite.ProdutoRepository;

import java.time.LocalDateTime;

public class ProdutoService {

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    public Produto cadastrar(Produto produto) {
        validar(produto);

        produto.setAtivo(true);
        produto.setCriadoEm(LocalDateTime.now());
        produto.setAtualizadoEm(LocalDateTime.now());

        return repository.salvar(produto);
    }

    private void validar(Produto produto) {
        if (produto.getNome() == null
                || produto.getNome().isBlank()) {
            throw new IllegalArgumentException(
                    "O nome do produto é obrigatório."
            );
        }

        if (produto.getPrecoVenda() == null
                || produto.getPrecoVenda().signum() < 0) {
            throw new IllegalArgumentException(
                    "O preço de venda é inválido."
            );
        }
    }
}