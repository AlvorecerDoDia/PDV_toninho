package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.StatusCaixa;
import br.com.loja.pdv.domain.model.Caixa;
import br.com.loja.pdv.domain.model.MovimentacaoCaixa;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.CaixaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Testa o caixa reduzido a abertura e fechamento. */
class CaixaServiceTest {
    private CaixaService service;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setAtivo(true);
        SessaoUsuario sessao = new SessaoUsuario();
        sessao.iniciar(usuario);
        service = new CaixaService(new MemoryRepository(), sessao);
    }

    @Test
    void deveAbrirEFecharCaixa() {
        Caixa caixa = service.abrir(new BigDecimal("50.00"));
        assertEquals(StatusCaixa.ABERTO, caixa.getStatus());
        assertEquals(new BigDecimal("50.00"), service.consultarDinheiroEsperado(caixa.getId()));

        Caixa fechado = service.fechar(new BigDecimal("50.00"));
        assertEquals(StatusCaixa.FECHADO, fechado.getStatus());
    }

    @Test
    void deveImpedirDoisCaixasAbertos() {
        service.abrir(BigDecimal.ZERO);
        assertThrows(ValidationException.class, () -> service.abrir(BigDecimal.ZERO));
    }

    private static final class MemoryRepository implements CaixaRepository {
        private Caixa caixa;
        private final List<MovimentacaoCaixa> movimentos = new ArrayList<>();
        @Override public Caixa abrir(Caixa novo, MovimentacaoCaixa abertura) {
            novo.setId(1L);
            abertura.setCaixaId(1L);
            caixa = novo;
            movimentos.add(abertura);
            return novo;
        }
        @Override public Optional<Caixa> buscarPorId(long id) { return Optional.ofNullable(caixa); }
        @Override public Optional<Caixa> buscarAbertoPorUsuario(long usuarioId) {
            return caixa != null && caixa.getStatus() == StatusCaixa.ABERTO
                    ? Optional.of(caixa) : Optional.empty();
        }
        @Override public Caixa fechar(long caixaId, BigDecimal valorContado, LocalDateTime fechadoEm) {
            caixa.setValorContado(valorContado);
            caixa.setStatus(StatusCaixa.FECHADO);
            caixa.setFechadoEm(fechadoEm);
            return caixa;
        }
        @Override public BigDecimal buscarDinheiroEsperado(long caixaId) {
            return movimentos.stream().map(MovimentacaoCaixa::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        @Override public List<MovimentacaoCaixa> listarMovimentacoes(long caixaId) {
            return List.copyOf(movimentos);
        }
    }
}
