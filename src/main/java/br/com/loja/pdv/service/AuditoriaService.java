package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.model.RegistroAuditoria;
import br.com.loja.pdv.repository.AuditoriaRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Registra acoes relevantes do usuario para rastreabilidade administrativa.
 */
public final class AuditoriaService {
    private final AuditoriaRepository repository;
    private final SessaoUsuario sessao;
    private final Clock clock;

    /** Recebe as dependencias necessarias para aplicar as regras deste caso de uso. */
    public AuditoriaService(AuditoriaRepository repository, SessaoUsuario sessao) {
        this(repository, sessao, Clock.systemDefaultZone());
    }

    /** Variante usada pelos testes para controlar o horario dos registros. */
    AuditoriaService(
            AuditoriaRepository repository, SessaoUsuario sessao, Clock clock) {
        this.repository = repository;
        this.sessao = sessao;
        this.clock = clock;
    }

    /** Monta e persiste um evento usando o usuario da sessao quando disponivel. */
    public RegistroAuditoria registrar(
            String acao, String entidade, Long entidadeId,
            String valoresAnteriores, String valoresNovos) {
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setUsuarioId(sessao.atual().map(usuario -> usuario.getId()).orElse(null));
        registro.setAcao(acao);
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setValoresAnteriores(valoresAnteriores);
        registro.setValoresNovos(valoresNovos);
        registro.setCriadoEm(LocalDateTime.now(clock));
        return repository.salvar(registro);
    }

    /** Retorna os eventos mais recentes para consulta administrativa. */
    public List<RegistroAuditoria> listarRecentes(int limite) {
        return repository.listarRecentes(Math.max(1, Math.min(limite, 1_000)));
    }
}
