package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.model.Caixa;
import br.com.loja.pdv.domain.model.MovimentacaoCaixa;
import br.com.loja.pdv.service.CaixaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Coordena a tela de abertura, movimentações e fechamento de caixa. */
public final class CaixaController {
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label statusLabel;
    @FXML private Label mensagemLabel;
    @FXML private TextField aberturaField;
    @FXML private TextField valorField;
    @FXML private TextField motivoField;
    @FXML private TextField contadoField;
    @FXML private TableView<MovimentacaoCaixa> movimentacoesTable;
    @FXML private TableColumn<MovimentacaoCaixa, String> dataColumn;
    @FXML private TableColumn<MovimentacaoCaixa, String> tipoColumn;
    @FXML private TableColumn<MovimentacaoCaixa, String> valorColumn;
    @FXML private TableColumn<MovimentacaoCaixa, String> motivoColumn;

    private final CaixaService service;

    public CaixaController(CaixaService service) {
        this.service = service;
    }

    @FXML
    private void initialize() {
        UiFormatters.moeda(aberturaField, valorField, contadoField);
        dataColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getCriadoEm().format(DATE_TIME)));
        tipoColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getTipo().name()));
        valorColumn.setCellValueFactory(row -> new SimpleStringProperty(
                CURRENCY.format(row.getValue().getValor())));
        motivoColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getMotivo() == null ? "" : row.getValue().getMotivo()));
        refresh();
    }

    @FXML
    private void open() {
        execute(() -> {
            service.abrir(parseMoney(aberturaField.getText()));
            aberturaField.clear();
            return "Caixa aberto.";
        });
    }

    @FXML
    private void supply() {
        execute(() -> {
            service.suprir(parseMoney(valorField.getText()), motivoField.getText());
            clearMovement();
            return "Suprimento registrado.";
        });
    }

    @FXML
    private void withdraw() {
        if (!confirm("Confirmar sangria",
                "Registrar a retirada informada do caixa?")) return;
        execute(() -> {
            service.sangrar(parseMoney(valorField.getText()), motivoField.getText());
            clearMovement();
            return "Sangria registrada.";
        });
    }

    @FXML
    private void close() {
        if (!confirm("Fechar caixa",
                "Após o fechamento, o caixa não aceitará novas vendas. Continuar?")) return;
        execute(() -> {
            Caixa caixa = service.fechar(parseMoney(contadoField.getText()));
            contadoField.clear();
            return "Caixa fechado. Esperado: " + CURRENCY.format(caixa.getValorEsperado())
                    + " | Diferença: " + CURRENCY.format(caixa.getDiferenca());
        });
    }

    @FXML
    private void refresh() {
        try {
            service.buscarCaixaAtual().ifPresentOrElse(
                    caixa -> statusLabel.setText(
                            "Caixa aberto desde " + caixa.getAbertoEm().format(DATE_TIME)),
                    () -> statusLabel.setText("Caixa fechado"));
            movimentacoesTable.getItems().setAll(service.listarMovimentacoesAtuais());
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    private void execute(Action action) {
        try {
            message(action.run(), false);
            refresh();
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    private BigDecimal parseMoney(String text) {
        String normalized = text == null ? "" : text.strip();
        if (normalized.contains(",")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(normalized);
    }

    private void clearMovement() {
        valorField.clear();
        motivoField.clear();
    }

    private void message(String text, boolean error) {
        mensagemLabel.setText(text == null ? "Ocorreu um erro." : text);
        mensagemLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }

    private boolean confirm(String title, String text) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    @FunctionalInterface
    private interface Action {
        String run();
    }
}
