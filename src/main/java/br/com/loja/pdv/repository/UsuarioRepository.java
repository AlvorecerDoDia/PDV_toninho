package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

/** Contrato de persistencia e autenticacao dos usuarios. */
public interface UsuarioRepository {
    /** Insere um usuario. */
    Usuario salvar(Usuario usuario);
    /** Atualiza um usuario. */
    void atualizar(Usuario usuario);
    /** Consulta por identificador. */
    Optional<Usuario> buscarPorId(long id);
    /** Consulta por login. */
    Optional<Usuario> buscarPorLogin(String login);
    /** Lista todos os usuarios. */
    List<Usuario> listar();
    /** Conta os usuarios existentes. */
    long contar();
}
