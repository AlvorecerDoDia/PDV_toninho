package br.com.loja.pdv.integration;

import br.com.loja.pdv.App;
import br.com.loja.pdv.controller.LoginController;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.sqlite.SQLiteUsuarioRepository;
import br.com.loja.pdv.service.AutenticacaoService;
import br.com.loja.pdv.service.SessaoUsuario;
import br.com.loja.pdv.service.UsuarioService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginFxmlTest {

    @TempDir
    Path tempDirectory;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        CountDownLatch initialized = new CountDownLatch(1);
        try {
            Platform.startup(initialized::countDown);
        } catch (IllegalStateException alreadyStarted) {
            initialized.countDown();
        }
        assertTrue(initialized.await(5, TimeUnit.SECONDS));
    }

    @Test
    void deveCarregarTelaDeLoginSemErro() throws InterruptedException {
        Database database = new Database(tempDirectory.resolve("login-fxml.db"));
        new DatabaseInitializer(database).initialize();
        SQLiteUsuarioRepository repository = new SQLiteUsuarioRepository(database);
        PasswordHasher hasher = new PasswordHasher();
        UsuarioService usuarios = new UsuarioService(repository, hasher);
        AutenticacaoService autenticacao =
                new AutenticacaoService(repository, hasher, new SessaoUsuario());
        AtomicReference<Parent> root = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch loaded = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        App.class.getResource("/br/com/loja/pdv/view/login-view.fxml")
                );
                loader.setControllerFactory(type -> {
                    if (type == LoginController.class) {
                        return new LoginController(autenticacao, usuarios, () -> { });
                    }
                    throw new IllegalArgumentException(type.getName());
                });
                root.set(loader.load());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                loaded.countDown();
            }
        });

        assertTrue(loaded.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError("FXML de login não carregou.", failure.get());
        }
        assertNotNull(root.get());
    }
}
