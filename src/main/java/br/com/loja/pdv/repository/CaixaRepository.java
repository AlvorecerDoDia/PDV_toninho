package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Caixa;
import br.com.loja.pdv.domain.model.MovimentacaoCaixa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Contrato transacional de caixas e suas movimentacoes financeiras. */
public interface CaixaRepository {
    /** Persiste o caixa e a movimentacao inicial de forma atomica. */
    Caixa abrir(Caixa caixa, MovimentacaoCaixa abertura);
    /** Consulta um caixa por identificador. */
    Optional<Caixa> buscarPorId(long id);
    /** Consulta o caixa aberto de um operador. */
    Optional<Caixa> buscarAbertoPorUsuario(long usuarioId);
    /** Persiste uma movimentacao no caixa. */
    MovimentacaoCaixa registrar(MovimentacaoCaixa movimentacao);
    /** Persiste os valores finais do fechamento. */
    Caixa fechar(long caixaId, BigDecimal valorContado, LocalDateTime fechadoEm);
    /** Calcula o dinheiro esperado no caixa. */
    BigDecimal buscarDinheiroEsperado(long caixaId);
    /** Lista as movimentacoes de um caixa. */
    List<MovimentacaoCaixa> listarMovimentacoes(long caixaId);
}
