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

/** Controla entradas, ajustes e a consulta do estoque. */
public final class EstoqueController {
    @FXML private ComboBox<Produto> produtoCombo;
    @FXML private TextField entradaField;
    @FXML private TextField novoSaldoField;
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

    public EstoqueController(
            EstoqueService estoqueService, ProdutoService produtoService) {
        this.estoqueService = estoqueService;
        this.produtoService = produtoService;
    }

    @FXML
    private void initialize() {
        UiFormatters.inteiro(entradaField, novoSaldoField);
        produtoCombo.getItems().setAll(produtoService.listarAtivos());
        produtoCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Produto produto) {
                return produto == null ? "" : produto.getNome();
            }
            @Override public Produto fromString(String texto) { return null; }
        });
        inicioPicker.setValue(LocalDate.now().minusMonths(1));
        fimPicker.setValue(LocalDate.now());
        dataColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getCriadoEm().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        tipoColumn.setCellValueFactory(row -> new SimpleStringProperty(
                nomeTipo(row.getValue().getTipo())));
        quantidadeColumn.setCellValueFactory(row -> new SimpleStringProperty(
                Integer.toString(row.getValue().getQuantidade())));
        saldoColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getQuantidadeAnterior() + " → "
                        + row.getValue().getQuantidadePosterior()));
        motivoColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getMotivo() == null ? "" : row.getValue().getMotivo()));
        produtoCombo.valueProperty().addListener(
                (observable, anterior, atual) -> refresh());
    }

    @FXML
    private void registerEntry() {
        Produto produto = produtoSelecionado();
        if (produto == null) return;
        try {
            estoqueService.registrarEntrada(
                    produto.getId(), Integer.parseInt(entradaField.getText()),
                    motivoArea.getText());
            entradaField.clear();
            motivoArea.clear();
            mensagem("Entrada registrada.", false);
            refresh();
        } catch (RuntimeException exception) {
            mensagem(ErrorHandler.mensagem(exception), true);
        }
    }

    @FXML
    private void adjustBalance() {
        Produto produto = produtoSelecionado();
        if (produto == null) return;
        try {
            estoqueService.ajustarSaldo(
                    produto.getId(), Integer.parseInt(novoSaldoField.getText()),
                    motivoArea.getText());
            novoSaldoField.clear();
            motivoArea.clear();
            mensagem("Saldo ajustado.", false);
            refresh();
        } catch (RuntimeException exception) {
            mensagem(ErrorHandler.mensagem(exception), true);
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
            int saldo = estoqueService.buscarSaldo(produto.getId());
            saldoLabel.setText("Saldo: " + saldo);
            saldoLabel.setStyle(saldo <= produto.getEstoqueMinimo()
                    ? "-fx-text-fill: #b91c1c; -fx-font-weight: bold;"
                    : "");
            historicoTable.getItems().setAll(estoqueService.listar(
                    produto.getId(), inicioPicker.getValue(), fimPicker.getValue()));
        } catch (RuntimeException exception) {
            mensagem(ErrorHandler.mensagem(exception), true);
        }
    }

    private Produto produtoSelecionado() {
        Produto produto = produtoCombo.getValue();
        if (produto == null) {
            mensagem("Selecione um produto.", true);
        }
        return produto;
    }

    private String nomeTipo(TipoMovimentacaoEstoque tipo) {
        return switch (tipo) {
            case ENTRADA -> "Entrada";
            case AJUSTE_POSITIVO, AJUSTE_NEGATIVO -> "Ajuste";
            case SAIDA_VENDA -> "Venda";
            case DEVOLUCAO -> "Devolução";
            case PERDA -> "Perda";
        };
    }

    private void mensagem(String texto, boolean erro) {
        mensagemLabel.setText(texto == null ? "Ocorreu um erro." : texto);
        mensagemLabel.setStyle(erro
                ? "-fx-text-fill: #b91c1c;"
                : "-fx-text-fill: #166534;");
    }
}
