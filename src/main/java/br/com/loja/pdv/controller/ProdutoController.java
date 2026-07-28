package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.service.ProdutoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;

public final class ProdutoController {
    @FXML private TextField codigoField;
    @FXML private TextField nomeField;
    @FXML private TextField custoField;
    @FXML private TextField vendaField;
    @FXML private TextField minimoField;
    @FXML private TextField pesquisaField;
    @FXML private TableView<Produto> tabela;
    @FXML private TableColumn<Produto, String> nomeColumn;
    @FXML private TableColumn<Produto, String> codigoColumn;
    @FXML private TableColumn<Produto, String> vendaColumn;
    @FXML private TableColumn<Produto, String> statusColumn;
    @FXML private Label mensagemLabel;
    @FXML private Button desativarButton;
    @FXML private Button reativarButton;

    private final ProdutoService service;
    private Produto selected;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @FXML
    private void initialize() {
        nomeColumn.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getNome()));
        codigoColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getCodigoBarras() == null ? "" : row.getValue().getCodigoBarras()));
        vendaColumn.setCellValueFactory(row -> new SimpleStringProperty(
                "R$ " + row.getValue().getPrecoVenda().toPlainString().replace('.', ',')));
        statusColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().isAtivo() ? "Ativo" : "Inativo"));
        tabela.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, value) -> select(value));
        pesquisaField.textProperty().addListener((observable, oldValue, value) -> refresh());
        tabela.setRowFactory(ignored -> new TableRow<>() {
            @Override protected void updateItem(Produto item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(!empty && item != null && !item.isAtivo() ? "-fx-opacity: 0.55;" : "");
            }
        });
        refresh();
    }

    @FXML
    private void save() {
        try {
            Produto produto = selected == null ? new Produto() : selected;
            produto.setCodigoBarras(codigoField.getText());
            produto.setNome(nomeField.getText());
            produto.setPrecoCusto(new BigDecimal(custoField.getText().replace(',', '.')));
            produto.setPrecoVenda(new BigDecimal(vendaField.getText().replace(',', '.')));
            produto.setEstoqueMinimo(Integer.parseInt(minimoField.getText()));
            if (selected == null) service.cadastrar(produto); else service.atualizar(produto);
            message("Produto salvo com sucesso.", false);
            clear();
            refresh();
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    @FXML private void deactivate() { changeStatus(false); }
    @FXML private void reactivate() { changeStatus(true); }

    private void changeStatus(boolean active) {
        if (selected == null) return;
        try {
            if (active) service.reativar(selected.getId()); else service.desativar(selected.getId());
            clear();
            refresh();
            message(active ? "Produto reativado." : "Produto desativado.", false);
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    @FXML
    private void clear() {
        selected = null;
        tabela.getSelectionModel().clearSelection();
        codigoField.clear(); nomeField.clear(); custoField.clear(); vendaField.clear();
        minimoField.clear();
        desativarButton.setDisable(true);
        reativarButton.setDisable(true);
    }

    private void select(Produto produto) {
        selected = produto;
        if (produto == null) return;
        codigoField.setText(produto.getCodigoBarras());
        nomeField.setText(produto.getNome());
        custoField.setText(produto.getPrecoCusto().toPlainString());
        vendaField.setText(produto.getPrecoVenda().toPlainString());
        minimoField.setText(Integer.toString(produto.getEstoqueMinimo()));
        desativarButton.setDisable(!produto.isAtivo());
        reativarButton.setDisable(produto.isAtivo());
    }

    private void refresh() {
        tabela.getItems().setAll(service.pesquisar(pesquisaField.getText()));
    }

    private void message(String text, boolean error) {
        mensagemLabel.setText(text == null ? "Ocorreu um erro." : text);
        mensagemLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }
}
