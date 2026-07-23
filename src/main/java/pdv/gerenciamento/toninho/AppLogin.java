package pdv.gerenciamento.toninho;

import org.kordamp.bootstrapfx.BootstrapFX;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class AppLogin extends Application {
    private AnchorPane pane;
    private TextField txLogin;
    private PasswordField txSenha;
    private Button btEntrar, btSair;

    private Stage stage;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage; // 1. Primeiro guardamos o stage recebido

        initComponents();   // 2. Construímos a tela (os botões e campos)
        initListeners();    // 3. Configuramos os cliques

        // 4. Configuração da Cena (Só agora usamos o pane, que já foi criado)
        Scene scene = new Scene(pane);
        stage.setScene(scene);
        stage.setTitle("Login - GolFX");
        stage.setResizable(false);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        // 5. Mostra a janela
        stage.show();

        // 6. Ajusta posições (Só funciona depois do stage.show)
        initLayout();
    }

    private void initComponents() {
        pane = new AnchorPane();
        pane.setPrefSize(400, 300);
        pane.setStyle("-fx-background-color: white;");

        txLogin = new TextField();
        txLogin.setPromptText("Digite seu login...");
        txLogin.getStyleClass().add("form-control");

        txSenha = new PasswordField();
        txSenha.setPromptText("Digite sua senha...");

        btEntrar = new Button("Entrar");
        // Dica: Adicionando estilo CSS simples direto no código para ficar mais bonito
        btEntrar.getStyleClass().setAll("btn", "btn-success");

        btSair = new Button("Sair");
        btSair.getStyleClass().setAll("btn", "btn-danger");

        pane.getChildren().addAll(txLogin, txSenha, btEntrar, btSair);

        // REMOVIDO: stage.setScene e stage.show daqui.
        // Quem manda abrir a janela é o método start(), não o initComponents().
    }

    private void initLayout() {
        // Centralizando manual (Cálculo: (LarguraTela - LarguraComponente) / 2)
        // Usamos valores fixos aproximados para simplificar, já que components ainda não têm largura fixa
        double centroX = (400 - 150) / 2.0;

        txLogin.setLayoutX(centroX);
        txLogin.setLayoutY(70);
        txLogin.setPrefWidth(150); // Força largura para o cálculo funcionar

        txSenha.setLayoutX(centroX);
        txSenha.setLayoutY(120);
        txSenha.setPrefWidth(150);

        btEntrar.setLayoutX(centroX);
        btEntrar.setLayoutY(170);
        btEntrar.setPrefWidth(150);

        btSair.setLayoutX(centroX);
        btSair.setLayoutY(220);
        btSair.setPrefWidth(150);
    }

    private void initListeners() {
        btSair.setOnAction(e -> fecharAplicacao());
        btEntrar.setOnAction(e -> logar());
    }

    private void logar() {
        if ("admin".equals(txLogin.getText()) && "123".equals(txSenha.getText())) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Sucesso");
            alert.setContentText("Login realizado!");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Erro de Acesso");
            alert.setContentText("Usuário ou senha incorretos.");
            alert.showAndWait();
        }
    }

    private void fecharAplicacao() {
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}