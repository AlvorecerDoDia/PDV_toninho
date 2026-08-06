package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.model.Categoria;
import br.com.loja.pdv.service.CategoriaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** Controla o cadastro e a renomeacao de categorias. */
public final class CategoriaController {
    @FXML private TextField nomeField;
    @FXML private TableView<Categoria> tabela;
    @FXML private TableColumn<Categoria, String> nomeColumn;
    @FXML private Label mensagemLabel;

    private final CategoriaService categorias;
    private Categoria selecionada;

    public CategoriaController(CategoriaService categorias) {
        this.categorias = categorias;
    }

    @FXML
    private void initialize() {
        nomeColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getNome()));
        tabela.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, atual) -> selecionar(atual));
        recarregar();
    }

    @FXML
    private void save() {
        try {
            if (selecionada == null) {
                categorias.cadastrar(nomeField.getText());
                mensagem("Categoria cadastrada.", false);
            } else {
                selecionada.setNome(nomeField.getText());
                categorias.atualizar(selecionada);
                mensagem("Categoria atualizada.", false);
            }
            clear();
            recarregar();
        } catch (RuntimeException exception) {
            mensagem(ErrorHandler.mensagem(exception), true);
        }
    }

    @FXML
    private void clear() {
        selecionada = null;
        tabela.getSelectionModel().clearSelection();
        nomeField.clear();
    }

    private void selecionar(Categoria categoria) {
        selecionada = categoria;
        if (categoria != null) nomeField.setText(categoria.getNome());
    }

    void recarregar() {
        if (tabela != null) tabela.getItems().setAll(categorias.listarTodas());
    }

    private void mensagem(String texto, boolean erro) {
        mensagemLabel.setText(texto == null ? "Ocorreu um erro." : texto);
        mensagemLabel.setStyle(erro
                ? "-fx-text-fill: #b91c1c;"
                : "-fx-text-fill: #166534;");
    }
}
