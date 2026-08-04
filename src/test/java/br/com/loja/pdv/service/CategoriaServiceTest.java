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

/** Testa as regras de negocio das categorias sem usar banco. */
class CategoriaServiceTest {
    private final CategoriaService service = new CategoriaService(new MemoryRepository());

    @Test
    void deveNormalizarNomeAoCadastrar() {
        Categoria categoria = service.cadastrar("  Material   Escolar  ");
        assertEquals("Material Escolar", categoria.getNome());
    }

    @Test
    void deveImpedirNomeVazio() {
        assertThrows(ValidationException.class, () -> service.cadastrar("  "));
    }

    @Test
    void deveImpedirNomeDuplicadoIgnorandoMaiusculas() {
        service.cadastrar("Papelaria");
        assertThrows(ValidationException.class, () -> service.cadastrar("papelaria"));
    }

    private static final class MemoryRepository implements CategoriaRepository {
        private final List<Categoria> values = new ArrayList<>();
        private long sequence = 1;

        @Override public Categoria salvar(Categoria categoria) {
            categoria.setId(sequence++);
            values.add(categoria);
            return categoria;
        }
        @Override public void atualizar(Categoria categoria) {}
        @Override public Optional<Categoria> buscarPorId(long id) {
            return values.stream().filter(value -> value.getId() == id).findFirst();
        }
        @Override public Optional<Categoria> buscarPorNome(String nome) {
            return values.stream().filter(value -> value.getNome().equalsIgnoreCase(nome)).findFirst();
        }
        @Override public List<Categoria> listarAtivas() {
            return values.stream().filter(Categoria::isAtiva).toList();
        }
        @Override public List<Categoria> listarTodas() { return List.copyOf(values); }
        @Override public void desativar(long id) { buscarPorId(id).orElseThrow().setAtiva(false); }
        @Override public void reativar(long id) { buscarPorId(id).orElseThrow().setAtiva(true); }
    }
}
