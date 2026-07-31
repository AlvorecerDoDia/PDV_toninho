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

/**
 * Centraliza cadastro, edicao, consulta e inativacao de produtos.
 */
public final class ProdutoService {

    private final ProdutoRepository repository;
    private final Clock clock;
    private final AuditoriaService auditoria;

    public ProdutoService(ProdutoRepository repository) {
        this(repository, null, Clock.systemDefaultZone());
    }

    public ProdutoService(ProdutoRepository repository, AuditoriaService auditoria) {
        this(repository, auditoria, Clock.systemDefaultZone());
    }

    ProdutoService(ProdutoRepository repository, Clock clock) {
        this(repository, null, clock);
    }

    ProdutoService(
            ProdutoRepository repository, AuditoriaService auditoria, Clock clock) {
        this.repository = repository;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    public Produto cadastrar(Produto produto) {
        normalizeAndValidate(produto);
        LocalDateTime now = LocalDateTime.now(clock);
        produto.setId(null);
        produto.setAtivo(true);
        produto.setCriadoEm(now);
        produto.setAtualizadoEm(now);
        return repository.salvar(produto);
    }

    public void atualizar(Produto produto) {
        if (produto == null || produto.getId() == null || produto.getId() <= 0) {
            throw new ValidationException("Selecione um produto válido para atualizar.");
        }
        Produto persisted = buscarPorId(produto.getId());
        normalizeAndValidate(produto);
        produto.setQuantidadeEstoque(persisted.getQuantidadeEstoque());
        produto.setAtivo(persisted.isAtivo());
        produto.setCriadoEm(persisted.getCriadoEm());
        produto.setAtualizadoEm(LocalDateTime.now(clock));
        repository.atualizar(produto);
        if (auditoria != null
                && (persisted.getPrecoCusto().compareTo(produto.getPrecoCusto()) != 0
                || persisted.getPrecoVenda().compareTo(produto.getPrecoVenda()) != 0)) {
            auditoria.registrar(
                    "ALTERACAO_PRECO", "PRODUTO", produto.getId(),
                    prices(persisted), prices(produto));
        }
    }

    public Produto buscarPorId(long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado."));
    }

    public Optional<Produto> buscarPorCodigoBarras(String codigo) {
        String normalized = normalizeBarcode(codigo);
        return normalized == null ? Optional.empty() : repository.buscarPorCodigoBarras(normalized);
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

    private void normalizeAndValidate(Produto produto) {
        if (produto == null) {
            throw new ValidationException("O produto é obrigatório.");
        }
        String name = produto.getNome() == null ? "" : produto.getNome().strip()
                .replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new ValidationException("O nome do produto é obrigatório.");
        }
        produto.setNome(name);
        produto.setCodigoBarras(normalizeBarcode(produto.getCodigoBarras()));
        produto.setPrecoCusto(validateMoney(produto.getPrecoCusto(), "preço de custo"));
        produto.setPrecoVenda(validateMoney(produto.getPrecoVenda(), "preço de venda"));
        if (produto.getQuantidadeEstoque() < 0) {
            throw new ValidationException("A quantidade inicial não pode ser negativa.");
        }
        if (produto.getEstoqueMinimo() < 0) {
            throw new ValidationException("O estoque mínimo não pode ser negativo.");
        }
    }

    private String normalizeBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return null;
        }
        return barcode.strip();
    }

    private BigDecimal validateMoney(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new ValidationException("O " + field + " não pode ser negativo.");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValidationException("O " + field + " deve ter no máximo duas casas decimais.");
        }
    }

    private String prices(Produto produto) {
        return "custo=" + produto.getPrecoCusto().toPlainString()
                + "; venda=" + produto.getPrecoVenda().toPlainString();
    }
}
