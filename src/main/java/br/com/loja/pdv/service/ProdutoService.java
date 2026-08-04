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
    private final CategoriaService categorias;
    private final Clock clock;
    private final AuditoriaService auditoria;

    /** Recebe as dependencias necessarias para aplicar as regras deste caso de uso. */
    public ProdutoService(ProdutoRepository repository) {
        this(repository, null, null, Clock.systemDefaultZone());
    }

    /** Recebe as dependencias necessarias para aplicar as regras deste caso de uso. */
    public ProdutoService(ProdutoRepository repository, AuditoriaService auditoria) {
        this(repository, null, auditoria, Clock.systemDefaultZone());
    }

    /** Exige categorias validas no fluxo real de cadastro da aplicacao. */
    public ProdutoService(
            ProdutoRepository repository,
            CategoriaService categorias,
            AuditoriaService auditoria) {
        this(repository, categorias, auditoria, Clock.systemDefaultZone());
    }

    /** Variante usada pelos testes sem auditoria e com relogio controlado. */
    ProdutoService(ProdutoRepository repository, Clock clock) {
        this(repository, null, null, clock);
    }

    /** Construtor completo que recebe repositorio, categorias, auditoria e relogio. */
    ProdutoService(
            ProdutoRepository repository,
            CategoriaService categorias,
            AuditoriaService auditoria,
            Clock clock) {
        this.repository = repository;
        this.categorias = categorias;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    /** Normaliza os campos, valida a quantidade inicial e prepara datas do novo produto. */
    public Produto cadastrar(Produto produto) {
        normalizeAndValidate(produto);
        LocalDateTime now = LocalDateTime.now(clock);
        produto.setId(null);
        produto.setAtivo(true);
        produto.setCriadoEm(now);
        produto.setAtualizadoEm(now);
        return repository.salvar(produto);
    }

    /** Atualiza dados cadastrais preservando o saldo controlado pelo estoque. */
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

    /** Retorna o produto ou gera erro quando ele nao existe. */
    public Produto buscarPorId(long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado."));
    }

    /** Normaliza o codigo antes de consultar o repositorio. */
    public Optional<Produto> buscarPorCodigoBarras(String codigo) {
        String normalized = normalizeBarcode(codigo);
        return normalized == null ? Optional.empty() : repository.buscarPorCodigoBarras(normalized);
    }

    /** Retorna produtos disponiveis para venda. */
    public List<Produto> listarAtivos() {
        return repository.listarAtivos();
    }

    /** Normaliza o termo e delega a pesquisa textual. */
    public List<Produto> pesquisar(String termo) {
        return repository.pesquisar(termo == null ? "" : termo.strip());
    }

    /** Exige permissao e retira o produto das vendas futuras. */
    public void desativar(long id) {
        buscarPorId(id);
        repository.desativar(id);
    }

    /** Exige permissao e disponibiliza novamente o produto. */
    public void reativar(long id) {
        buscarPorId(id);
        repository.reativar(id);
    }

    /** Aplica todas as regras comuns de nome, codigo, precos e estoque minimo. */
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
        if (categorias != null) {
            if (produto.getCategoria() == null
                    || produto.getCategoria().getId() == null
                    || produto.getCategoria().getId() <= 0) {
                throw new ValidationException("Selecione uma categoria para o produto.");
            }
            var categoria = categorias.buscarPorId(produto.getCategoria().getId());
            if (!categoria.isAtiva()) {
                throw new ValidationException("A categoria selecionada está inativa.");
            }
            produto.setCategoria(categoria);
        }
        produto.setPrecoCusto(validateMoney(produto.getPrecoCusto(), "preço de custo"));
        produto.setPrecoVenda(validateMoney(produto.getPrecoVenda(), "preço de venda"));
        if (produto.getQuantidadeEstoque() < 0) {
            throw new ValidationException("A quantidade inicial não pode ser negativa.");
        }
        if (produto.getEstoqueMinimo() < 0) {
            throw new ValidationException("O estoque mínimo não pode ser negativo.");
        }
    }

    /** Converte codigo vazio em NULL e remove espacos externos. */
    private String normalizeBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return null;
        }
        return barcode.strip();
    }

    /** Exige valor monetario nao negativo com duas casas exatas. */
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

    /** Monta uma representacao curta dos precos para auditoria. */
    private String prices(Produto produto) {
        return "custo=" + produto.getPrecoCusto().toPlainString()
                + "; venda=" + produto.getPrecoVenda().toPlainString();
    }
}
