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

/** Controla abertura, consulta e fechamento do caixa. */
public final class CaixaController {
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label statusLabel;
    @FXML private Label esperadoLabel;
    @FXML private Label mensagemLabel;
    @FXML private TextField aberturaField;
    @FXML private TextField contadoField;
    @FXML private TableView<MovimentacaoCaixa> movimentacoesTable;
    @FXML private TableColumn<MovimentacaoCaixa, String> dataColumn;
    @FXML private TableColumn<MovimentacaoCaixa, String> tipoColumn;
    @FXML private TableColumn<MovimentacaoCaixa, String> valorColumn;

    private final CaixaService service;

    public CaixaController(CaixaService service) {
        this.service = service;
    }

    @FXML
    private void initialize() {
        UiFormatters.moeda(aberturaField, contadoField);
        dataColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getCriadoEm().format(DATE_TIME)));
        tipoColumn.setCellValueFactory(row -> new SimpleStringProperty(
                switch (row.getValue().getTipo()) {
                    case ABERTURA -> "Abertura";
                    case VENDA_DINHEIRO -> "Venda";
                    case ESTORNO -> "Estorno";
                    case SUPRIMENTO -> "Entrada antiga";
                    case SANGRIA -> "Retirada antiga";
                }));
        valorColumn.setCellValueFactory(row -> new SimpleStringProperty(
                CURRENCY.format(row.getValue().getValor())));
        refresh();
    }

    @FXML
    private void open() {
        executar(() -> {
            service.abrir(parseMoney(aberturaField.getText()));
            aberturaField.clear();
            return "Caixa aberto.";
        });
    }

    @FXML
    private void close() {
        Alert alerta = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Fechar o caixa atual?",
                ButtonType.YES,
                ButtonType.NO);
        alerta.setHeaderText(null);
        if (alerta.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        executar(() -> {
            Caixa caixa = service.fechar(parseMoney(contadoField.getText()));
            contadoField.clear();
            return "Caixa fechado. Esperado: " + CURRENCY.format(caixa.getValorEsperado())
                    + " | Diferença: " + CURRENCY.format(caixa.getDiferenca());
        });
    }

    @FXML
    private void refresh() {
        try {
            service.buscarCaixaAtual().ifPresentOrElse(caixa -> {
                statusLabel.setText("Aberto desde " + caixa.getAbertoEm().format(DATE_TIME));
                esperadoLabel.setText("Dinheiro esperado: "
                        + CURRENCY.format(service.consultarDinheiroEsperado(caixa.getId())));
            }, () -> {
                statusLabel.setText("Caixa fechado");
                esperadoLabel.setText("Dinheiro esperado: —");
            });
            movimentacoesTable.getItems().setAll(service.listarMovimentacoesAtuais());
        } catch (RuntimeException exception) {
            mensagem(ErrorHandler.mensagem(exception), true);
        }
    }

    private void executar(Acao acao) {
        try {
            mensagem(acao.executar(), false);
            refresh();
        } catch (RuntimeException exception) {
            mensagem(ErrorHandler.mensagem(exception), true);
        }
    }

    private BigDecimal parseMoney(String texto) {
        String normalizado = texto == null ? "" : texto.strip();
        if (normalizado.contains(",")) {
            normalizado = normalizado.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(normalizado);
    }

    private void mensagem(String texto, boolean erro) {
        mensagemLabel.setText(texto == null ? "Ocorreu um erro." : texto);
        mensagemLabel.setStyle(erro
                ? "-fx-text-fill: #b91c1c;"
                : "-fx-text-fill: #166534;");
    }

    @FunctionalInterface
    private interface Acao {
        String executar();
    }
}
