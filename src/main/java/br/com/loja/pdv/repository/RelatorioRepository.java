package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.enums.TipoRelatorio;
import br.com.loja.pdv.domain.model.FiltroRelatorio;
import br.com.loja.pdv.domain.model.LinhaRelatorio;

import java.util.List;

/** Contrato das consultas consolidadas usadas nos relatorios. */
public interface RelatorioRepository {
    /** Executa a consulta consolidada do tipo solicitado. */
    List<LinhaRelatorio> gerar(TipoRelatorio tipo, FiltroRelatorio filtro);
}
