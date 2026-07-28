package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

/** Contrato de persistência e autenticação dos usuários. */
public interface UsuarioRepository {
    Usuario salvar(Usuario usuario);
    void atualizar(Usuario usuario);
    Optional<Usuario> buscarPorId(long id);
    Optional<Usuario> buscarPorLogin(String login);
    List<Usuario> listar();
    long contar();
}
