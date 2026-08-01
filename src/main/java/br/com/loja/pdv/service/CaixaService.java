package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.Permissao;
import br.com.loja.pdv.domain.enums.StatusCaixa;
import br.com.loja.pdv.domain.enums.TipoMovimentacaoCaixa;
import br.com.loja.pdv.domain.model.Caixa;
import br.com.loja.pdv.domain.model.MovimentacaoCaixa;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.CaixaRepository;
import br.com.loja.pdv.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Aplica as regras de abertura, movimentacao e fechamento do caixa.
 */
public final class CaixaService {
    private final CaixaRepository repository;
    private final SessaoUsuario sessao;
    private final Clock clock;
    private final AuditoriaService auditoria;

    /** Recebe as dependencias necessarias para aplicar as regras deste caso de uso. */
    public CaixaService(CaixaRepository repository, SessaoUsuario sessao) {
        this(repository, sessao, null, Clock.systemDefaultZone());
    }

    /** Recebe as dependencias necessarias para aplicar as regras deste caso de uso. */
    public CaixaService(
            CaixaRepository repository, SessaoUsuario sessao, AuditoriaService auditoria) {
        this(repository, sessao, auditoria, Clock.systemDefaultZone());
    }

    /** Variante usada pelos testes sem auditoria e com relogio controlado. */
    CaixaService(CaixaRepository repository, SessaoUsuario sessao, Clock clock) {
        this(repository, sessao, null, clock);
    }

    /** Construtor completo que recebe todas as dependencias configuraveis. */
    CaixaService(
            CaixaRepository repository, SessaoUsuario sessao,
            AuditoriaService auditoria, Clock clock) {
        this.repository = repository;
        this.sessao = sessao;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    /** Valida permissao, valor e ausencia de outro caixa aberto para o operador. */
    public Caixa abrir(BigDecimal valorAbertura) {
        Usuario usuario = usuarioAtual();
        validateMoney(valorAbertura, true);
        if (repository.buscarAbertoPorUsuario(usuario.getId()).isPresent()) {
            throw new ValidationException("O usuário já possui um caixa aberto.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Caixa caixa = new Caixa();
        caixa.setUsuarioId(usuario.getId());
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setValorAbertura(valorAbertura);
        caixa.setAbertoEm(now);

        MovimentacaoCaixa abertura = movement(
                caixa, usuario, TipoMovimentacaoCaixa.ABERTURA, valorAbertura,
                "Abertura do caixa", now);
        return repository.abrir(caixa, abertura);
    }

    /** Registra uma entrada de dinheiro no caixa atual. */
    public MovimentacaoCaixa suprir(BigDecimal valor, String motivo) {
        return registrar(TipoMovimentacaoCaixa.SUPRIMENTO, valor, motivo);
    }

    /** Registra uma retirada depois de validar valor, motivo e saldo esperado. */
    public MovimentacaoCaixa sangrar(BigDecimal valor, String motivo) {
        return registrar(TipoMovimentacaoCaixa.SANGRIA, valor, motivo);
    }

    /** Calcula esperado e diferenca antes de concluir o caixa. */
    public Caixa fechar(BigDecimal valorContado) {
        Usuario usuario = usuarioAtual();
        validateMoney(valorContado, true);
        Caixa caixa = caixaAberto(usuario.getId());
        return repository.fechar(caixa.getId(), valorContado, LocalDateTime.now(clock));
    }

    /** Retorna o caixa aberto do usuario autenticado. */
    public Optional<Caixa> buscarCaixaAtual() {
        Usuario usuario = usuarioAtual();
        return repository.buscarAbertoPorUsuario(usuario.getId());
    }

    /** Lista o historico do caixa aberto ou retorna lista vazia. */
    public List<MovimentacaoCaixa> listarMovimentacoesAtuais() {
        Usuario usuario = usuarioAtual();
        Optional<Caixa> caixa = repository.buscarAbertoPorUsuario(usuario.getId());
        return caixa.map(value -> repository.listarMovimentacoes(value.getId()))
                .orElseGet(List::of);
    }

    /** Consulta o valor fisico esperado do caixa atual. */
    public BigDecimal consultarDinheiroEsperado(long caixaId) {
        sessao.exigir(Permissao.RELATORIOS);
        return repository.buscarDinheiroEsperado(caixaId);
    }

    /** Compartilha a criacao das movimentacoes manuais. */
    private MovimentacaoCaixa registrar(
            TipoMovimentacaoCaixa tipo, BigDecimal valor, String motivo) {
        Usuario usuario = usuarioAtual();
        validateMoney(valor, false);
        String normalizedReason = normalizeReason(motivo);
        Caixa caixa = caixaAberto(usuario.getId());
        MovimentacaoCaixa movement = movement(
                caixa, usuario, tipo, valor, normalizedReason, LocalDateTime.now(clock));
        MovimentacaoCaixa registrada = repository.registrar(movement);
        if (auditoria != null
                && (tipo == TipoMovimentacaoCaixa.SANGRIA
                || tipo == TipoMovimentacaoCaixa.SUPRIMENTO)) {
            auditoria.registrar(
                    tipo.name(), "CAIXA", caixa.getId(), null,
                    "valor=" + valor.toPlainString() + "; motivo=" + normalizedReason);
        }
        return registrada;
    }

    /** Exige uma sessao valida e retorna o usuario autenticado. */
    private Usuario usuarioAtual() {
        sessao.exigir(Permissao.CAIXA);
        return sessao.atual().orElseThrow();
    }

    /** Exige que o operador possua caixa aberto. */
    private Caixa caixaAberto(long usuarioId) {
        return repository.buscarAbertoPorUsuario(usuarioId)
                .orElseThrow(() -> new ValidationException("Abra o caixa para continuar."));
    }

    /** Monta uma movimentacao com dados comuns e horario atual. */
    private MovimentacaoCaixa movement(
            Caixa caixa, Usuario usuario, TipoMovimentacaoCaixa tipo,
            BigDecimal valor, String motivo, LocalDateTime timestamp) {
        MovimentacaoCaixa movement = new MovimentacaoCaixa();
        if (caixa.getId() != null) movement.setCaixaId(caixa.getId());
        movement.setUsuarioId(usuario.getId());
        movement.setTipo(tipo);
        movement.setValor(valor);
        movement.setMotivo(motivo);
        movement.setCriadoEm(timestamp);
        return movement;
    }

    /** Remove espacos extras e valida justificativas obrigatorias. */
    private String normalizeReason(String value) {
        String normalized = value == null ? "" : value.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw new ValidationException("Informe o motivo da movimentação.");
        }
        return normalized;
    }

    /** Normaliza e valida valores monetarios positivos ou nao negativos. */
    private void validateMoney(BigDecimal value, boolean allowZero) {
        if (value == null || value.signum() < 0 || (!allowZero && value.signum() == 0)) {
            throw new ValidationException(allowZero
                    ? "O valor não pode ser negativo."
                    : "O valor deve ser maior que zero.");
        }
        try {
            MoneyUtils.toCents(value);
        } catch (ArithmeticException exception) {
            throw new ValidationException("O valor deve possuir no máximo duas casas decimais.");
        }
    }
}
