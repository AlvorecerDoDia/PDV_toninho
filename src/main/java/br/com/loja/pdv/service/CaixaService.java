package br.com.loja.pdv.service;

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

/** Controla somente abertura, vendas em dinheiro e fechamento. */
public final class CaixaService {
    private final CaixaRepository repository;
    private final SessaoUsuario sessao;
    private final Clock clock;

    public CaixaService(CaixaRepository repository, SessaoUsuario sessao) {
        this(repository, sessao, Clock.systemDefaultZone());
    }

    CaixaService(CaixaRepository repository, SessaoUsuario sessao, Clock clock) {
        this.repository = repository;
        this.sessao = sessao;
        this.clock = clock;
    }

    public Caixa abrir(BigDecimal valorAbertura) {
        Usuario usuario = sessao.exigirLogin();
        validarDinheiro(valorAbertura);
        if (repository.buscarAbertoPorUsuario(usuario.getId()).isPresent()) {
            throw new ValidationException("O usuário já possui um caixa aberto.");
        }
        LocalDateTime agora = LocalDateTime.now(clock);
        Caixa caixa = new Caixa();
        caixa.setUsuarioId(usuario.getId());
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setValorAbertura(valorAbertura);
        caixa.setAbertoEm(agora);

        MovimentacaoCaixa abertura = new MovimentacaoCaixa();
        abertura.setUsuarioId(usuario.getId());
        abertura.setTipo(TipoMovimentacaoCaixa.ABERTURA);
        abertura.setValor(valorAbertura);
        abertura.setMotivo("Abertura do caixa");
        abertura.setCriadoEm(agora);
        return repository.abrir(caixa, abertura);
    }

    public Caixa fechar(BigDecimal valorContado) {
        Usuario usuario = sessao.exigirLogin();
        validarDinheiro(valorContado);
        Caixa caixa = repository.buscarAbertoPorUsuario(usuario.getId())
                .orElseThrow(() -> new ValidationException("Não existe caixa aberto."));
        return repository.fechar(caixa.getId(), valorContado, LocalDateTime.now(clock));
    }

    public Optional<Caixa> buscarCaixaAtual() {
        return repository.buscarAbertoPorUsuario(sessao.exigirLogin().getId());
    }

    public List<MovimentacaoCaixa> listarMovimentacoesAtuais() {
        return buscarCaixaAtual()
                .map(caixa -> repository.listarMovimentacoes(caixa.getId()))
                .orElseGet(List::of);
    }

    public BigDecimal consultarDinheiroEsperado(long caixaId) {
        sessao.exigirLogin();
        return repository.buscarDinheiroEsperado(caixaId);
    }

    private void validarDinheiro(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new ValidationException("O valor não pode ser negativo.");
        }
        try {
            MoneyUtils.toCents(valor);
        } catch (ArithmeticException exception) {
            throw new ValidationException("O valor deve possuir no máximo duas casas decimais.");
        }
    }
}
