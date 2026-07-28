package br.com.loja.pdv;

import br.com.loja.pdv.controller.*;
import br.com.loja.pdv.domain.model.CarrinhoVenda;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.infrastructure.printing.FormatadorComprovante;
import br.com.loja.pdv.infrastructure.printing.ImpressoraWindows;
import br.com.loja.pdv.infrastructure.reporting.ExportadorCsv;
import br.com.loja.pdv.infrastructure.backup.GerenciadorBackup;
import br.com.loja.pdv.infrastructure.logging.LoggingConfigurator;
import br.com.loja.pdv.repository.sqlite.*;
import br.com.loja.pdv.service.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {
    private Database database;
    private SQLiteUsuarioRepository userRepository;
    private PasswordHasher passwordHasher;
    private UsuarioService userService;
    private AutenticacaoService authenticationService;
    private SessaoUsuario session;
    private SQLiteProdutoRepository productRepository;
    private SQLiteCaixaRepository cashRepository;
    private CarrinhoVenda saleCart;
    private PagamentoService paymentService;
    private VendaService saleService;
    private BackupService backupService;
    private AuditoriaService auditService;
    private String generatedInitialPassword;

    @Override
    public void init() {
        LoggingConfigurator.configurar(Path.of("logs"));
        Thread.setDefaultUncaughtExceptionHandler(ErrorHandler::registrarInesperado);
        database = Database.local();
        new DatabaseInitializer(database).initialize();
        userRepository = new SQLiteUsuarioRepository(database);
        passwordHasher = new PasswordHasher();
        session = new SessaoUsuario();
        auditService = new AuditoriaService(
                new SQLiteAuditoriaRepository(database), session);
        userService = new UsuarioService(userRepository, passwordHasher, auditService);
        authenticationService = new AutenticacaoService(
                userRepository, passwordHasher, session);
        productRepository = new SQLiteProdutoRepository(database);
        cashRepository = new SQLiteCaixaRepository(database);
        saleCart = new CarrinhoVenda();
        paymentService = new PagamentoService();
        saleService = new VendaService(
                new SQLiteVendaRepository(database), productRepository, cashRepository,
                session, paymentService);
        backupService = new BackupService(
                new GerenciadorBackup(database, Path.of("backups")), session, auditService);
        if (userRepository.contar() == 0) {
            String configured = System.getenv("PDV_ADMIN_PASSWORD");
            generatedInitialPassword = configured == null || configured.length() < 8
                    ? generateInitialPassword() : configured;
            userService.criarAdministradorInicial(generatedInitialPassword.toCharArray());
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        showLogin(stage);
        stage.setTitle("PDV Toninho");
        stage.show();
        if (generatedInitialPassword != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Primeiro acesso");
            alert.setHeaderText("Administrador inicial criado");
            alert.setContentText("Login: admin\nSenha temporária: "
                    + generatedInitialPassword
                    + "\nA troca será exigida no primeiro acesso.");
            alert.showAndWait();
            generatedInitialPassword = null;
        }
    }

    private void showLogin(Stage stage) throws IOException {
        FXMLLoader loader = loader("/br/com/loja/pdv/view/login-view.fxml");
        loader.setControllerFactory(type -> {
            if (type == LoginController.class) {
                return new LoginController(
                        authenticationService, userService, () -> showMainUnchecked(stage));
            }
            throw unconfigured(type);
        });
        stage.setScene(new Scene(loader.load(), 460, 360));
        stage.setMinWidth(460);
        stage.setMinHeight(360);
    }

    private void showMainUnchecked(Stage stage) {
        try {
            showMain(stage);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível abrir a tela principal.", exception);
        }
    }

    private void showMain(Stage stage) throws IOException {
        FXMLLoader loader = loader("/br/com/loja/pdv/view/main-view.fxml");
        loader.setControllerFactory(type -> createMainController(type));
        stage.setScene(new Scene(loader.load(), 1280, 720));
        stage.setMinWidth(1024);
        stage.setMinHeight(640);
        stage.centerOnScreen();
    }

    private Object createMainController(Class<?> type) {
        ProdutoService productService = new ProdutoService(productRepository, auditService);
        if (type == MainController.class) {
            return new MainController(
                    session, new CaixaService(cashRepository, session, auditService));
        }
        if (type == VendaController.class) {
            return new VendaController(productService, session, saleCart);
        }
        if (type == PagamentoController.class) {
            return new PagamentoController(paymentService, saleService, saleCart);
        }
        if (type == HistoricoVendaController.class) {
            FormatadorComprovante formatter = new FormatadorComprovante("PDV Toninho");
            return new HistoricoVendaController(
                    saleService, userService, new SQLitePagamentoRepository(database),
                    formatter, new ImpressoraWindows(formatter));
        }
        if (type == RelatorioController.class) {
            return new RelatorioController(
                    new RelatorioService(
                            new SQLiteRelatorioRepository(database), session),
                    userService, productService, new ExportadorCsv());
        }
        if (type == BackupController.class) return new BackupController(backupService);
        if (type == ProdutoController.class) return new ProdutoController(productService);
        if (type == EstoqueController.class) {
            return new EstoqueController(
                    new EstoqueService(
                            new SQLiteEstoqueRepository(database), productRepository,
                            session, auditService),
                    productService);
        }
        if (type == CaixaController.class) {
            return new CaixaController(
                    new CaixaService(cashRepository, session, auditService));
        }
        if (type == UsuarioController.class) return new UsuarioController(userService, session);
        throw unconfigured(type);
    }

    private FXMLLoader loader(String resource) {
        return new FXMLLoader(Objects.requireNonNull(App.class.getResource(resource)));
    }

    private IllegalArgumentException unconfigured(Class<?> type) {
        return new IllegalArgumentException("Controller não configurado: " + type.getName());
    }

    private String generateInitialPassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() {
        if (backupService == null) return;
        try {
            backupService.criarAutomatico();
        } catch (RuntimeException exception) {
            Logger.getLogger(App.class.getName()).log(
                    Level.SEVERE, "Falha ao criar backup automático no encerramento.", exception);
        }
    }
}
