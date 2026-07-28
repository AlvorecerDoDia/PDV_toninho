package br.com.loja.pdv;

import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class App extends Application {

    @Override
    public void init() {
        DatabaseInitializer.initialize();
    }
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(
                        App.class.getResource("/br/com/loja/pdv/view/main-view.fxml")
                )
        );

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
