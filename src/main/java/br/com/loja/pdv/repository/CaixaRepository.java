package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Caixa;
import br.com.loja.pdv.domain.model.MovimentacaoCaixa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Contrato do caixa simplificado. */
public interface CaixaRepository {
    Caixa abrir(Caixa caixa, MovimentacaoCaixa abertura);
    Optional<Caixa> buscarPorId(long id);
    Optional<Caixa> buscarAbertoPorUsuario(long usuarioId);
    Caixa fechar(long caixaId, BigDecimal valorContado, LocalDateTime fechadoEm);
    BigDecimal buscarDinheiroEsperado(long caixaId);
    List<MovimentacaoCaixa> listarMovimentacoes(long caixaId);
}
