package br.com.loja.pdv.integration;

import br.com.loja.pdv.App;
import br.com.loja.pdv.controller.ProdutoController;
import br.com.loja.pdv.controller.EstoqueController;
import br.com.loja.pdv.controller.MainController;
import br.com.loja.pdv.controller.UsuarioController;
import br.com.loja.pdv.controller.CaixaController;
import br.com.loja.pdv.controller.VendaController;
import br.com.loja.pdv.controller.PagamentoController;
import br.com.loja.pdv.controller.HistoricoVendaController;
import br.com.loja.pdv.controller.RelatorioController;
import br.com.loja.pdv.controller.BackupController;
import br.com.loja.pdv.domain.model.CarrinhoVenda;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.repository.sqlite.SQLiteProdutoRepository;
import br.com.loja.pdv.repository.sqlite.SQLiteEstoqueRepository;
import br.com.loja.pdv.repository.sqlite.SQLiteUsuarioRepository;
import br.com.loja.pdv.repository.sqlite.SQLiteCaixaRepository;
import br.com.loja.pdv.repository.sqlite.SQLiteVendaRepository;
import br.com.loja.pdv.repository.sqlite.SQLitePagamentoRepository;
import br.com.loja.pdv.repository.sqlite.SQLiteRelatorioRepository;
import br.com.loja.pdv.service.EstoqueService;
import br.com.loja.pdv.service.SessaoUsuario;
import br.com.loja.pdv.service.UsuarioService;
import br.com.loja.pdv.service.CaixaService;
import br.com.loja.pdv.service.PagamentoService;
import br.com.loja.pdv.service.VendaService;
import br.com.loja.pdv.service.RelatorioService;
import br.com.loja.pdv.service.BackupService;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.infrastructure.printing.FormatadorComprovante;
import br.com.loja.pdv.infrastructure.printing.ImpressoraWindows;
import br.com.loja.pdv.infrastructure.reporting.ExportadorCsv;
import br.com.loja.pdv.infrastructure.backup.GerenciadorBackup;
import br.com.loja.pdv.service.ProdutoService;
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

class ProdutoFxmlTest {

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
    void deveCarregarTelaDeProdutosSemErro() throws InterruptedException {
        Database database = new Database(tempDirectory.resolve("fxml.db"));
        new DatabaseInitializer(database).initialize();
        AtomicReference<Parent> root = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch loaded = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                SQLiteProdutoRepository products = new SQLiteProdutoRepository(database);
                ProdutoService productService = new ProdutoService(products);
                UsuarioService userService = new UsuarioService(
                        new SQLiteUsuarioRepository(database), new PasswordHasher());
                SessaoUsuario session = new SessaoUsuario();
                session.iniciar(userService.criarAdministradorInicial(
                        "SenhaForte1".toCharArray()));
                SQLiteCaixaRepository cashRegisters = new SQLiteCaixaRepository(database);
                CarrinhoVenda cart = new CarrinhoVenda();
                PagamentoService paymentService = new PagamentoService();
                VendaService saleService = new VendaService(
                        new SQLiteVendaRepository(database), products, cashRegisters,
                        session, paymentService);
                FXMLLoader loader = new FXMLLoader(
                        App.class.getResource("/br/com/loja/pdv/view/main-view.fxml")
                );
                loader.setControllerFactory(type -> {
                    if (type == MainController.class) return new MainController(session);
                    if (type == VendaController.class) {
                        return new VendaController(productService, session, cart);
                    }
                    if (type == PagamentoController.class) {
                        return new PagamentoController(paymentService, saleService, cart);
                    }
                    if (type == HistoricoVendaController.class) {
                        FormatadorComprovante formatter =
                                new FormatadorComprovante("PDV Toninho");
                        return new HistoricoVendaController(
                                saleService, userService,
                                new SQLitePagamentoRepository(database),
                                formatter, new ImpressoraWindows(formatter));
                    }
                    if (type == RelatorioController.class) {
                        return new RelatorioController(
                                new RelatorioService(
                                        new SQLiteRelatorioRepository(database), session),
                                userService, productService, new ExportadorCsv());
                    }
                    if (type == BackupController.class) {
                        return new BackupController(new BackupService(
                                new GerenciadorBackup(
                                        database, tempDirectory.resolve("backups")),
                                session));
                    }
                    if (type == ProdutoController.class) {
                        return new ProdutoController(productService);
                    }
                    if (type == EstoqueController.class) {
                        return new EstoqueController(
                                new EstoqueService(
                                        new SQLiteEstoqueRepository(database), products),
                                productService
                        );
                    }
                    if (type == CaixaController.class) {
                        return new CaixaController(
                                new CaixaService(cashRegisters, session));
                    }
                    if (type == UsuarioController.class) {
                        return new UsuarioController(userService, session);
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
            throw new AssertionError("FXML não carregou.", failure.get());
        }
        assertNotNull(root.get());
    }
}
