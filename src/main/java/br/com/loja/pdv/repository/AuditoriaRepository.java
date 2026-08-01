package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.RegistroAuditoria;

import java.util.List;

/** Contrato de gravacao e consulta dos eventos de auditoria. */
public interface AuditoriaRepository {
    /** Persiste um evento de auditoria. */
    RegistroAuditoria salvar(RegistroAuditoria registro);
    /** Consulta os eventos mais recentes. */
    List<RegistroAuditoria> listarRecentes(int limite);
}
