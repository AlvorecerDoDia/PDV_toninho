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

/** Oferece apenas entrada e ajuste direto do saldo. */
public final class EstoqueService {
    private final EstoqueRepository estoqueRepository;
    private final ProdutoRepository produtoRepository;
    private final SessaoUsuario sessao;
    private final Clock clock;

    public EstoqueService(
            EstoqueRepository estoqueRepository, ProdutoRepository produtoRepository) {
        this(estoqueRepository, produtoRepository, null, Clock.systemDefaultZone());
    }

    public EstoqueService(
            EstoqueRepository estoqueRepository, ProdutoRepository produtoRepository,
            SessaoUsuario sessao) {
        this(estoqueRepository, produtoRepository, sessao, Clock.systemDefaultZone());
    }

    EstoqueService(
            EstoqueRepository estoqueRepository, ProdutoRepository produtoRepository,
            Clock clock) {
        this(estoqueRepository, produtoRepository, null, clock);
    }

    EstoqueService(
            EstoqueRepository estoqueRepository, ProdutoRepository produtoRepository,
            SessaoUsuario sessao, Clock clock) {
        this.estoqueRepository = estoqueRepository;
        this.produtoRepository = produtoRepository;
        this.sessao = sessao;
        this.clock = clock;
    }

    public MovimentacaoEstoque registrarEntrada(
            long produtoId, int quantidade, String motivo) {
        if (quantidade <= 0) {
            throw new ValidationException("A quantidade deve ser maior que zero.");
        }
        return registrar(produtoId, TipoMovimentacaoEstoque.ENTRADA,
                quantidade, normalizarMotivoOpcional(motivo));
    }

    public MovimentacaoEstoque ajustarSaldo(
            long produtoId, int novoSaldo, String motivo) {
        if (novoSaldo < 0) {
            throw new ValidationException("O novo saldo não pode ser negativo.");
        }
        String motivoNormalizado = normalizarMotivoObrigatorio(motivo);
        int saldoAtual = estoqueRepository.buscarSaldo(produtoId);
        if (novoSaldo == saldoAtual) {
            throw new ValidationException("O novo saldo é igual ao saldo atual.");
        }
        int diferenca = Math.abs(novoSaldo - saldoAtual);
        TipoMovimentacaoEstoque tipo = novoSaldo > saldoAtual
                ? TipoMovimentacaoEstoque.AJUSTE_POSITIVO
                : TipoMovimentacaoEstoque.AJUSTE_NEGATIVO;
        return registrar(produtoId, tipo, diferenca, motivoNormalizado);
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
                produtoId,
                inicio.atStartOfDay(),
                fim.plusDays(1).atStartOfDay().minusNanos(1));
    }

    private MovimentacaoEstoque registrar(
            long produtoId, TipoMovimentacaoEstoque tipo,
            int quantidade, String motivo) {
        Produto produto = produtoRepository.buscarPorId(produtoId)
                .orElseThrow(() -> new ValidationException("Produto não encontrado."));
        if (!produto.isAtivo()) {
            throw new ValidationException("O produto está inativo.");
        }
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setProdutoId(produtoId);
        movimentacao.setTipo(tipo);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setMotivo(motivo);
        if (sessao != null) {
            movimentacao.setUsuarioId(sessao.atual()
                    .map(usuario -> usuario.getId()).orElse(null));
        }
        movimentacao.setCriadoEm(LocalDateTime.now(clock));
        return estoqueRepository.registrar(movimentacao);
    }

    private String normalizarMotivoOpcional(String motivo) {
        String normalizado = motivo == null ? "" : motivo.strip().replaceAll("\\s+", " ");
        return normalizado.isEmpty() ? null : normalizado;
    }

    private String normalizarMotivoObrigatorio(String motivo) {
        String normalizado = normalizarMotivoOpcional(motivo);
        if (normalizado == null) {
            throw new ValidationException("Informe o motivo do ajuste.");
        }
        return normalizado;
    }
}
