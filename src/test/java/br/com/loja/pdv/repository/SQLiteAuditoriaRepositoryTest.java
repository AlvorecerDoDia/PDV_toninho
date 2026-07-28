package br.com.loja.pdv.repository;

import br.com.loja.pdv.domain.model.RegistroAuditoria;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.repository.sqlite.SQLiteAuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteAuditoriaRepositoryTest {
    @TempDir Path tempDirectory;
    private SQLiteAuditoriaRepository repository;

    @BeforeEach
    void setUp() {
        Database database = new Database(tempDirectory.resolve("auditoria.db"));
        new DatabaseInitializer(database).initialize();
        repository = new SQLiteAuditoriaRepository(database);
    }

    @Test
    void deveSalvarEListarRegistroSemDadosSensiveis() {
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setAcao("ALTERACAO_USUARIO");
        registro.setEntidade("USUARIO");
        registro.setEntidadeId(12L);
        registro.setValoresAnteriores("perfil=OPERADOR");
        registro.setValoresNovos("perfil=GERENTE");
        registro.setCriadoEm(LocalDateTime.now());

        RegistroAuditoria salvo = repository.salvar(registro);
        RegistroAuditoria consultado = repository.listarRecentes(10).getFirst();

        assertNotNull(salvo.getId());
        assertEquals("ALTERACAO_USUARIO", consultado.getAcao());
        assertEquals(12L, consultado.getEntidadeId());
        assertFalse(consultado.getValoresNovos().toLowerCase().contains("senha"));
        assertNull(consultado.getUsuarioId());
    }
}
