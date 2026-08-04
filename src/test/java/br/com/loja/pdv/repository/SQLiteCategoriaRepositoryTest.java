package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.Categoria;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.repository.sqlite.SQLiteCategoriaRepository;
import br.com.loja.pdv.service.CategoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa a persistencia e a situacao das categorias. */
class SQLiteCategoriaRepositoryTest {
    @TempDir
    Path tempDirectory;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        Database database = new Database(tempDirectory.resolve("categorias.db"));
        new DatabaseInitializer(database).initialize();
        service = new CategoriaService(new SQLiteCategoriaRepository(database));
    }

    @Test
    void deveListarCategoriasIniciais() {
        assertTrue(service.listarAtivas().stream()
                .anyMatch(categoria -> categoria.getNome().equals("Papelaria")));
        assertTrue(service.listarAtivas().stream()
                .anyMatch(categoria -> categoria.getNome().equals("Brinquedos")));
    }

    @Test
    void deveCadastrarEConsultarCategoria() {
        Categoria categoria = service.cadastrar("  Eletrônicos  ");

        assertTrue(categoria.getId() > 0);
        assertEquals("Eletrônicos", service.buscarPorId(categoria.getId()).getNome());
    }

    @Test
    void deveDesativarEReativarCategoria() {
        Categoria categoria = service.cadastrar("Festas");

        service.desativar(categoria.getId());
        assertFalse(service.buscarPorId(categoria.getId()).isAtiva());

        service.reativar(categoria.getId());
        assertTrue(service.buscarPorId(categoria.getId()).isAtiva());
    }
}
