package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.TipoMovimentacaoEstoque;
import br.com.loja.pdv.domain.model.MovimentacaoEstoque;
import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.EstoqueRepository;
import br.com.loja.pdv.repository.ProdutoRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class EstoqueService {
    private final EstoqueRepository estoqueRepository;
    private final ProdutoRepository produtoRepository;
    private final Clock clock;

    public EstoqueService(
            EstoqueRepository estoqueRepository, ProdutoRepository produtoRepository) {
        this(estoqueRepository, produtoRepository, Clock.systemDefaultZone());
    }

    EstoqueService(
            EstoqueRepository estoqueRepository, ProdutoRepository produtoRepository, Clock clock) {
        this.estoqueRepository = estoqueRepository;
        this.produtoRepository = produtoRepository;
        this.clock = clock;
    }

    public MovimentacaoEstoque registrar(
            long produtoId, TipoMovimentacaoEstoque tipo, int quantidade, String motivo) {
        Produto produto = produtoRepository.buscarPorId(produtoId)
                .orElseThrow(() -> new ValidationException("Produto não encontrado."));
        if (!produto.isAtivo()) throw new ValidationException("O produto está inativo.");
        if (tipo == null) throw new ValidationException("O tipo de movimentação é obrigatório.");
        if (tipo == TipoMovimentacaoEstoque.SAIDA_VENDA) {
            throw new ValidationException("Saída de venda só pode ser feita pela finalização da venda.");
        }
        if (quantidade <= 0) throw new ValidationException("A quantidade deve ser maior que zero.");
        String normalizedReason = motivo == null ? null : motivo.strip().replaceAll("\\s+", " ");
        if (tipo.isReasonRequired() && (normalizedReason == null || normalizedReason.isBlank())) {
            throw new ValidationException("Informe o motivo da movimentação.");
        }

        MovimentacaoEstoque movement = new MovimentacaoEstoque();
        movement.setProdutoId(produtoId);
        movement.setTipo(tipo);
        movement.setQuantidade(quantidade);
        movement.setMotivo(normalizedReason == null || normalizedReason.isBlank()
                ? null : normalizedReason);
        movement.setCriadoEm(LocalDateTime.now(clock));
        return estoqueRepository.registrar(movement);
    }

    public int buscarSaldo(long produtoId) {
        return estoqueRepository.buscarSaldo(produtoId);
    }

    public List<MovimentacaoEstoque> listar(
            long produtoId, LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new ValidationException("Informe um período válido.");
        }
        return estoqueRepository.listar(
                produtoId, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay().minusNanos(1));
    }
}
