package br.com.loja.pdv;

import br.com.loja.pdv.controller.ProdutoController;
import br.com.loja.pdv.controller.EstoqueController;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.repository.sqlite.SQLiteProdutoRepository;
import br.com.loja.pdv.repository.sqlite.SQLiteEstoqueRepository;
import br.com.loja.pdv.service.EstoqueService;
import br.com.loja.pdv.service.ProdutoService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class App extends Application {
    private Database database;

    @Override
    public void init() {
        database = Database.local();
        new DatabaseInitializer(database).initialize();
    }
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(
                        App.class.getResource("/br/com/loja/pdv/view/main-view.fxml")
                )
        );
        loader.setControllerFactory(type -> {
            SQLiteProdutoRepository products = new SQLiteProdutoRepository(database);
            ProdutoService productService = new ProdutoService(products);
            if (type == ProdutoController.class) {
                return new ProdutoController(productService);
            }
            if (type == EstoqueController.class) {
                return new EstoqueController(
                        new EstoqueService(new SQLiteEstoqueRepository(database), products),
                        productService
                );
            }
            throw new IllegalArgumentException("Controller não configurado: " + type.getName());
        });

        Scene scene = new Scene(loader.load(), 1000, 650);

        stage.setTitle("PDV da Loja");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
