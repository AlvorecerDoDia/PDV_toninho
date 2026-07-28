package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.enums.TipoMovimentacaoEstoque;
import br.com.loja.pdv.domain.model.MovimentacaoEstoque;
import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.service.EstoqueService;
import br.com.loja.pdv.service.ProdutoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Liga os campos de estoque ao histórico e às regras do serviço. */
public final class EstoqueController {
    @FXML private ComboBox<Produto> produtoCombo;
    @FXML private ComboBox<TipoMovimentacaoEstoque> tipoCombo;
    @FXML private TextField quantidadeField;
    @FXML private TextArea motivoArea;
    @FXML private Label saldoLabel;
    @FXML private Label mensagemLabel;
    @FXML private DatePicker inicioPicker;
    @FXML private DatePicker fimPicker;
    @FXML private TableView<MovimentacaoEstoque> historicoTable;
    @FXML private TableColumn<MovimentacaoEstoque, String> dataColumn;
    @FXML private TableColumn<MovimentacaoEstoque, String> tipoColumn;
    @FXML private TableColumn<MovimentacaoEstoque, String> quantidadeColumn;
    @FXML private TableColumn<MovimentacaoEstoque, String> saldoColumn;
    @FXML private TableColumn<MovimentacaoEstoque, String> motivoColumn;

    private final EstoqueService estoqueService;
    private final ProdutoService produtoService;

    public EstoqueController(EstoqueService estoqueService, ProdutoService produtoService) {
        this.estoqueService = estoqueService;
        this.produtoService = produtoService;
    }

    @FXML
    private void initialize() {
        UiFormatters.inteiro(quantidadeField);
        produtoCombo.getItems().setAll(produtoService.listarAtivos());
        produtoCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Produto produto) {
                return produto == null ? "" : produto.getNome();
            }
            @Override public Produto fromString(String text) { return null; }
        });
        tipoCombo.getItems().setAll(
                TipoMovimentacaoEstoque.ENTRADA,
                TipoMovimentacaoEstoque.AJUSTE_POSITIVO,
                TipoMovimentacaoEstoque.AJUSTE_NEGATIVO,
                TipoMovimentacaoEstoque.DEVOLUCAO,
                TipoMovimentacaoEstoque.PERDA
        );
        inicioPicker.setValue(LocalDate.now().minusMonths(1));
        fimPicker.setValue(LocalDate.now());
        dataColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getCriadoEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        tipoColumn.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getTipo().name()));
        quantidadeColumn.setCellValueFactory(row -> new SimpleStringProperty(
                Integer.toString(row.getValue().getQuantidade())));
        saldoColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getQuantidadeAnterior() + " → " + row.getValue().getQuantidadePosterior()));
        motivoColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getMotivo() == null ? "" : row.getValue().getMotivo()));
        produtoCombo.valueProperty().addListener((observable, oldValue, value) -> refresh());
    }

    @FXML
    private void register() {
        Produto produto = produtoCombo.getValue();
        if (produto == null) {
            message("Selecione um produto.", true);
            return;
        }
        try {
            estoqueService.registrar(
                    produto.getId(), tipoCombo.getValue(),
                    Integer.parseInt(quantidadeField.getText()), motivoArea.getText());
            quantidadeField.clear();
            motivoArea.clear();
            message("Movimentação registrada.", false);
            refresh();
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    @FXML
    private void refresh() {
        Produto produto = produtoCombo.getValue();
        if (produto == null) {
            saldoLabel.setText("Saldo: —");
            historicoTable.getItems().clear();
            return;
        }
        try {
            int balance = estoqueService.buscarSaldo(produto.getId());
            saldoLabel.setText("Saldo: " + balance);
            saldoLabel.setStyle(balance <= produto.getEstoqueMinimo()
                    ? "-fx-text-fill: #b91c1c; -fx-font-weight: bold;" : "");
            historicoTable.getItems().setAll(estoqueService.listar(
                    produto.getId(), inicioPicker.getValue(), fimPicker.getValue()));
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    private void message(String text, boolean error) {
        mensagemLabel.setText(text == null ? "Ocorreu um erro." : text);
        mensagemLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }
}
