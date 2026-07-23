package pdv.gerenciamento.toninho;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import pdv.gerenciamento.toninho.controller.Carrinho;

public class VitrineApp extends Application {
    private AnchorPane pane;
    private TextField txPesquisa;
    private TableView<ItensProrperty> tbVitrine;
    private TableColumn<ItensProrperty, String> columnProduto;
    private TableColumn<ItensProrperty, Double> columnPreco;
    private static ObservableList<ItensProrperty> listItens = FXCollections.observableArrayList();
    private static Carrinho carrinho;

    public void start(Stage stage) throws Exception {

    }

    private void initComponents() {
        pane = new AnchorPane();
        pane.setPrefSize(800, 600);
        txPesquisa = new TextField();
        txPesquisa.setPromptText("Digite o item para pesquisa");
        tbVitrine = new TableView<ItensProrperty>();
        tbVitrine.setPrefSize(780, 550);
        columnProduto = new TableColumn<ItensProrperty, String>();
        columnPreco = new TableColumn<ItensProrperty, Double>();
        tbVitrine.getColumns().addAll(columnProduto, columnPreco);
        pane.getChildren().addAll(txPesquisa, tbVitrine);
        carrinho = new Carrinho();
        columnProduto.setCellValueFactory(new PropertyValueFactory<ItensProrperty, String>("produto"));
        columnPreco.setCellValueFactory(new PropertyValueFactory<ItensProrperty, Double>("preco"));
    }
    private void initItens(){

    }
    public static void main(String[] args) {
        launch(args);
    }
}