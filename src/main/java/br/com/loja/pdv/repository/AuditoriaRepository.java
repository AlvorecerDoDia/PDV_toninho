package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.RegistroAuditoria;

import java.util.List;

/** Contrato de gravacao e consulta dos eventos de auditoria. */
public interface AuditoriaRepository {
    RegistroAuditoria salvar(RegistroAuditoria registro);
    List<RegistroAuditoria> listarRecentes(int limite);
}
