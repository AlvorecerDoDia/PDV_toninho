package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.model.Categoria;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.CategoriaRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Testa o cadastro simples de categorias. */
class CategoriaServiceTest {
    private final MemoryRepository repository = new MemoryRepository();
    private final CategoriaService service = new CategoriaService(repository);

    @Test
    void deveCadastrarERenomearCategoria() {
        Categoria categoria = service.cadastrar("  Material   Escolar  ");
        assertEquals("Material Escolar", categoria.getNome());

        categoria.setNome("Papelaria escolar");
        service.atualizar(categoria);
        assertEquals("Papelaria escolar", service.buscarPorId(categoria.getId()).getNome());
    }

    @Test
    void deveImpedirNomeDuplicado() {
        service.cadastrar("Papelaria");
        assertThrows(ValidationException.class, () -> service.cadastrar("papelaria"));
    }

    private static final class MemoryRepository implements CategoriaRepository {
        private final List<Categoria> categorias = new ArrayList<>();

        @Override public Categoria salvar(Categoria categoria) {
            categoria.setId((long) categorias.size() + 1);
            categorias.add(categoria);
            return categoria;
        }
        @Override public void atualizar(Categoria categoria) { }
        @Override public Optional<Categoria> buscarPorId(long id) {
            return categorias.stream().filter(c -> c.getId() == id).findFirst();
        }
        @Override public Optional<Categoria> buscarPorNome(String nome) {
            return categorias.stream()
                    .filter(c -> c.getNome().equalsIgnoreCase(nome)).findFirst();
        }
        @Override public List<Categoria> listarTodas() { return List.copyOf(categorias); }
    }
}
