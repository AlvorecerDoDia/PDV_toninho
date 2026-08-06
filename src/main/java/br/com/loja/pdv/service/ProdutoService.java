package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.exception.EntityNotFoundException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Centraliza cadastro, edicao, consulta e situacao dos produtos. */
public final class ProdutoService {
    private final ProdutoRepository repository;
    private final CategoriaService categorias;
    private final Clock clock;

    public ProdutoService(ProdutoRepository repository) {
        this(repository, null, Clock.systemDefaultZone());
    }

    public ProdutoService(ProdutoRepository repository, CategoriaService categorias) {
        this(repository, categorias, Clock.systemDefaultZone());
    }

    ProdutoService(ProdutoRepository repository, Clock clock) {
        this(repository, null, clock);
    }

    ProdutoService(
            ProdutoRepository repository, CategoriaService categorias, Clock clock) {
        this.repository = repository;
        this.categorias = categorias;
        this.clock = clock;
    }

    public Produto cadastrar(Produto produto) {
        normalizarEValidar(produto);
        LocalDateTime agora = LocalDateTime.now(clock);
        produto.setId(null);
        produto.setAtivo(true);
        produto.setCriadoEm(agora);
        produto.setAtualizadoEm(agora);
        return repository.salvar(produto);
    }

    public void atualizar(Produto produto) {
        if (produto == null || produto.getId() == null || produto.getId() <= 0) {
            throw new ValidationException("Selecione um produto válido para atualizar.");
        }
        Produto persistido = buscarPorId(produto.getId());
        normalizarEValidar(produto);
        produto.setQuantidadeEstoque(persistido.getQuantidadeEstoque());
        produto.setAtivo(persistido.isAtivo());
        produto.setCriadoEm(persistido.getCriadoEm());
        produto.setAtualizadoEm(LocalDateTime.now(clock));
        repository.atualizar(produto);
    }

    public Produto buscarPorId(long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado."));
    }

    public Optional<Produto> buscarPorCodigoBarras(String codigo) {
        String normalizado = normalizarCodigo(codigo);
        return normalizado == null
                ? Optional.empty()
                : repository.buscarPorCodigoBarras(normalizado);
    }

    public List<Produto> listarAtivos() {
        return repository.listarAtivos();
    }

    public List<Produto> pesquisar(String termo) {
        return repository.pesquisar(termo == null ? "" : termo.strip());
    }

    public void desativar(long id) {
        buscarPorId(id);
        repository.desativar(id);
    }

    public void reativar(long id) {
        buscarPorId(id);
        repository.reativar(id);
    }

    private void normalizarEValidar(Produto produto) {
        if (produto == null) throw new ValidationException("O produto é obrigatório.");
        String nome = produto.getNome() == null
                ? ""
                : produto.getNome().strip().replaceAll("\\s+", " ");
        if (nome.isEmpty()) throw new ValidationException("O nome do produto é obrigatório.");
        produto.setNome(nome);
        produto.setCodigoBarras(normalizarCodigo(produto.getCodigoBarras()));
        if (categorias != null) {
            if (produto.getCategoria() == null
                    || produto.getCategoria().getId() == null
                    || produto.getCategoria().getId() <= 0) {
                throw new ValidationException("Selecione uma categoria para o produto.");
            }
            produto.setCategoria(categorias.buscarPorId(produto.getCategoria().getId()));
        }
        produto.setPrecoCusto(validarDinheiro(produto.getPrecoCusto(), "preço de custo"));
        produto.setPrecoVenda(validarDinheiro(produto.getPrecoVenda(), "preço de venda"));
        if (produto.getQuantidadeEstoque() < 0) {
            throw new ValidationException("A quantidade inicial não pode ser negativa.");
        }
        if (produto.getEstoqueMinimo() < 0) {
            throw new ValidationException("O estoque mínimo não pode ser negativo.");
        }
    }

    private String normalizarCodigo(String codigo) {
        return codigo == null || codigo.isBlank() ? null : codigo.strip();
    }

    private BigDecimal validarDinheiro(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0) {
            throw new ValidationException("O " + campo + " não pode ser negativo.");
        }
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValidationException(
                    "O " + campo + " deve ter no máximo duas casas decimais.");
        }
    }
}
