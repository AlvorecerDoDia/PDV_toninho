package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa usuarios sem perfis e sem troca obrigatoria no primeiro acesso. */
class UsuarioServiceTest {
    private final MemoryRepository repository = new MemoryRepository();
    private final UsuarioService service = new UsuarioService(repository, new PasswordHasher());

    @Test
    void deveCriarAdministradorInicialUmaUnicaVez() {
        assertTrue(service.configurarUsuarioInicialPadrao());
        assertFalse(service.configurarUsuarioInicialPadrao());
        assertEquals("admin", repository.listar().getFirst().getLogin());
    }

    @Test
    void deveCriarEAtualizarUsuarioSimples() {
        Usuario usuario = service.criar("  Maria  ", " MARIA ", "1234".toCharArray());
        assertEquals("Maria", usuario.getNome());
        assertEquals("maria", usuario.getLogin());

        service.atualizar(usuario.getId(), "Maria Silva", "maria.silva", false);
        assertFalse(service.buscar(usuario.getId()).isAtivo());
    }

    @Test
    void deveExigirSenhaMinima() {
        assertThrows(ValidationException.class,
                () -> service.criar("Joao", "joao", "123".toCharArray()));
    }

    private static final class MemoryRepository implements UsuarioRepository {
        private final List<Usuario> usuarios = new ArrayList<>();
        @Override public Usuario salvar(Usuario usuario) {
            usuario.setId((long) usuarios.size() + 1);
            usuarios.add(usuario);
            return usuario;
        }
        @Override public void atualizar(Usuario usuario) { }
        @Override public Optional<Usuario> buscarPorId(long id) {
            return usuarios.stream().filter(u -> u.getId() == id).findFirst();
        }
        @Override public Optional<Usuario> buscarPorLogin(String login) {
            return usuarios.stream().filter(u -> u.getLogin().equals(login)).findFirst();
        }
        @Override public List<Usuario> listar() { return List.copyOf(usuarios); }
        @Override public long contar() { return usuarios.size(); }
    }
}
