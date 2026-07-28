package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.RegistroAuditoria;

import java.util.List;

public interface AuditoriaRepository {
    RegistroAuditoria salvar(RegistroAuditoria registro);
    List<RegistroAuditoria> listarRecentes(int limite);
}
