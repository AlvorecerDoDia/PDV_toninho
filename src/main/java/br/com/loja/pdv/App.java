package br.com.loja.pdv;

import br.com.loja.pdv.config.AppPaths;
import br.com.loja.pdv.controller.*;
import br.com.loja.pdv.domain.model.CarrinhoVenda;
import br.com.loja.pdv.infrastructure.backup.GerenciadorBackup;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.infrastructure.logging.LoggingConfigurator;
import br.com.loja.pdv.infrastructure.printing.FormatadorComprovante;
import br.com.loja.pdv.infrastructure.printing.ImpressoraWindows;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.sqlite.*;
import br.com.loja.pdv.service.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Monta as dependencias e controla login, telas e backup automatico. */
public class App extends Application {
    private Database database;
    private UsuarioService usuarioService;
    private AutenticacaoService autenticacaoService;
    private SessaoUsuario sessao;
    private SQLiteProdutoRepository produtoRepository;
    private CategoriaService categoriaService;
    private SQLiteCaixaRepository caixaRepository;
    private CarrinhoVenda carrinho;
    private PagamentoService pagamentoService;
    private VendaService vendaService;
    private GerenciadorBackup backup;
    private boolean usuarioInicialCriado;

    @Override
    public void init() {
        LoggingConfigurator.configurar(AppPaths.logDirectory());
        Thread.setDefaultUncaughtExceptionHandler(ErrorHandler::registrarInesperado);
        database = Database.local();
        new DatabaseInitializer(database).initialize();

        SQLiteUsuarioRepository usuarioRepository =
                new SQLiteUsuarioRepository(database);
        PasswordHasher hasher = new PasswordHasher();
        sessao = new SessaoUsuario();
        usuarioService = new UsuarioService(usuarioRepository, hasher);
        autenticacaoService = new AutenticacaoService(
                usuarioRepository, hasher, sessao);
        produtoRepository = new SQLiteProdutoRepository(database);
        categoriaService = new CategoriaService(
                new SQLiteCategoriaRepository(database));
        caixaRepository = new SQLiteCaixaRepository(database);
        carrinho = new CarrinhoVenda();
        pagamentoService = new PagamentoService();
        vendaService = new VendaService(
                new SQLiteVendaRepository(database),
                produtoRepository,
                caixaRepository,
                sessao,
                pagamentoService);
        backup = new GerenciadorBackup(database, AppPaths.backupDirectory());
        usuarioInicialCriado = usuarioService.configurarUsuarioInicialPadrao();
    }

    @Override
    public void start(Stage stage) throws IOException {
        mostrarLogin(stage);
        stage.setTitle("PDV Toninho");
        stage.show();
        if (usuarioInicialCriado) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Primeiro acesso");
            alert.setHeaderText("Usuário inicial criado");
            alert.setContentText("Login: admin\nSenha: admin"
                    + "\nA senha pode ser alterada na tela Usuários.");
            alert.showAndWait();
            usuarioInicialCriado = false;
        }
    }

    private void mostrarLogin(Stage stage) throws IOException {
        FXMLLoader loader = loader("/br/com/loja/pdv/view/login-view.fxml");
        loader.setControllerFactory(tipo -> {
            if (tipo == LoginController.class) {
                return new LoginController(
                        autenticacaoService, () -> mostrarPrincipalSemExcecao(stage));
            }
            throw naoConfigurado(tipo);
        });
        stage.setScene(new Scene(loader.load(), 760, 520));
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.centerOnScreen();
    }

    private void mostrarPrincipalSemExcecao(Stage stage) {
        try {
            mostrarPrincipal(stage);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível abrir a tela principal.", exception);
        }
    }

    private void mostrarPrincipal(Stage stage) throws IOException {
        FXMLLoader loader = loader("/br/com/loja/pdv/view/main-view.fxml");
        loader.setControllerFactory(this::criarControllerPrincipal);
        stage.setScene(new Scene(loader.load(), 1366, 768));
        stage.setMinWidth(1180);
        stage.setMinHeight(700);
        stage.centerOnScreen();
    }

    private Object criarControllerPrincipal(Class<?> tipo) {
        ProdutoService produtos = new ProdutoService(
                produtoRepository, categoriaService);
        CaixaService caixa = new CaixaService(caixaRepository, sessao);
        if (tipo == MainController.class) return new MainController(sessao, caixa);
        if (tipo == VendaController.class) {
            return new VendaController(produtos, sessao, carrinho);
        }
        if (tipo == PagamentoController.class) {
            return new PagamentoController(
                    pagamentoService, vendaService, carrinho);
        }
        if (tipo == HistoricoVendaController.class) {
            FormatadorComprovante formatador =
                    new FormatadorComprovante("PDV Toninho");
            return new HistoricoVendaController(
                    vendaService,
                    usuarioService,
                    new SQLitePagamentoRepository(database),
                    formatador,
                    new ImpressoraWindows(formatador));
        }
        if (tipo == HistoricoProdutoController.class) {
            return new HistoricoProdutoController(vendaService, categoriaService);
        }
        if (tipo == CadastroController.class) return new CadastroController();
        if (tipo == CategoriaController.class) {
            return new CategoriaController(categoriaService);
        }
        if (tipo == ProdutoController.class) {
            return new ProdutoController(produtos, categoriaService);
        }
        if (tipo == EstoqueController.class) {
            return new EstoqueController(
                    new EstoqueService(
                            new SQLiteEstoqueRepository(database),
                            produtoRepository,
                            sessao),
                    produtos);
        }
        if (tipo == CaixaController.class) return new CaixaController(caixa);
        if (tipo == UsuarioController.class) {
            return new UsuarioController(usuarioService);
        }
        throw naoConfigurado(tipo);
    }

    private FXMLLoader loader(String recurso) {
        return new FXMLLoader(Objects.requireNonNull(App.class.getResource(recurso)));
    }

    private IllegalArgumentException naoConfigurado(Class<?> tipo) {
        return new IllegalArgumentException(
                "Controller não configurado: " + tipo.getName());
    }

    @Override
    public void stop() {
        if (backup == null) return;
        try {
            backup.criarAutomatico();
            backup.aplicarRetencao(10);
        } catch (RuntimeException exception) {
            Logger.getLogger(App.class.getName()).log(
                    Level.SEVERE,
                    "Falha ao criar backup automático no encerramento.",
                    exception);
        }
    }
}
