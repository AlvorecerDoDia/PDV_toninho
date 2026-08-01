package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.Permissao;
import br.com.loja.pdv.domain.enums.TipoRelatorio;
import br.com.loja.pdv.domain.model.FiltroRelatorio;
import br.com.loja.pdv.domain.model.LinhaRelatorio;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.RelatorioRepository;

import java.util.List;

/**
 * Valida acesso e periodo antes de solicitar a geracao dos relatorios.
 */
public final class RelatorioService {
    private final RelatorioRepository repository;
    private final SessaoUsuario sessao;

    /** Recebe as dependencias necessarias para aplicar as regras deste caso de uso. */
    public RelatorioService(RelatorioRepository repository, SessaoUsuario sessao) {
        this.repository = repository;
        this.sessao = sessao;
    }

    /** Valida permissao e intervalo antes de consultar o relatorio. */
    public List<LinhaRelatorio> gerar(TipoRelatorio tipo, FiltroRelatorio filtro) {
        sessao.exigir(Permissao.RELATORIOS);
        if (tipo == null) throw new ValidationException("Selecione um relatório.");
        if (filtro == null || filtro.inicio() == null || filtro.fim() == null
                || filtro.inicio().isAfter(filtro.fim())) {
            throw new ValidationException("Informe um período válido.");
        }
        return repository.gerar(tipo, filtro);
    }
}
