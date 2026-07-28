package br.com.loja.pdv;

import br.com.loja.pdv.controller.*;
import br.com.loja.pdv.domain.model.CarrinhoVenda;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.infrastructure.printing.FormatadorComprovante;
import br.com.loja.pdv.infrastructure.printing.ImpressoraWindows;
import br.com.loja.pdv.repository.sqlite.*;
import br.com.loja.pdv.service.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

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
    private String generatedInitialPassword;

    @Override
    public void init() {
        database = Database.local();
        new DatabaseInitializer(database).initialize();
        userRepository = new SQLiteUsuarioRepository(database);
        passwordHasher = new PasswordHasher();
        userService = new UsuarioService(userRepository, passwordHasher);
        session = new SessaoUsuario();
        authenticationService = new AutenticacaoService(
                userRepository, passwordHasher, session);
        productRepository = new SQLiteProdutoRepository(database);
        cashRepository = new SQLiteCaixaRepository(database);
        saleCart = new CarrinhoVenda();
        paymentService = new PagamentoService();
        saleService = new VendaService(
                new SQLiteVendaRepository(database), productRepository, cashRepository,
                session, paymentService);
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
        stage.setScene(new Scene(loader.load(), 1100, 700));
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.centerOnScreen();
    }

    private Object createMainController(Class<?> type) {
        ProdutoService productService = new ProdutoService(productRepository);
        if (type == MainController.class) return new MainController(session);
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
        if (type == ProdutoController.class) return new ProdutoController(productService);
        if (type == EstoqueController.class) {
            return new EstoqueController(
                    new EstoqueService(
                            new SQLiteEstoqueRepository(database), productRepository),
                    productService);
        }
        if (type == CaixaController.class) {
            return new CaixaController(new CaixaService(cashRepository, session));
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
}
