package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.enums.FormaPagamento;
import br.com.loja.pdv.domain.enums.TipoRelatorio;
import br.com.loja.pdv.domain.model.*;
import br.com.loja.pdv.infrastructure.reporting.ExportadorCsv;
import br.com.loja.pdv.service.ProdutoService;
import br.com.loja.pdv.service.RelatorioService;
import br.com.loja.pdv.service.UsuarioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Aplica filtros, exibe resultados e exporta relatorios para CSV. */
public final class RelatorioController {
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private ComboBox<TipoRelatorio> tipoCombo;
    @FXML private DatePicker inicioPicker;
    @FXML private DatePicker fimPicker;
    @FXML private ComboBox<Usuario> operadorCombo;
    @FXML private ComboBox<FormaPagamento> formaCombo;
    @FXML private ComboBox<Produto> produtoCombo;
    @FXML private Label mensagemLabel;
    @FXML private TableView<LinhaRelatorio> tabela;
    @FXML private TableColumn<LinhaRelatorio, String> categoriaColumn;
    @FXML private TableColumn<LinhaRelatorio, String> detalheColumn;
    @FXML private TableColumn<LinhaRelatorio, String> quantidadeColumn;
    @FXML private TableColumn<LinhaRelatorio, String> valorColumn;
    @FXML private TableColumn<LinhaRelatorio, String> valor2Column;
    @FXML private TableColumn<LinhaRelatorio, String> dataColumn;

    private final RelatorioService service;
    private final UsuarioService usuarios;
    private final ProdutoService produtos;
    private final ExportadorCsv exporter;
    private List<LinhaRelatorio> currentRows = List.of();

    public RelatorioController(
            RelatorioService service, UsuarioService usuarios,
            ProdutoService produtos, ExportadorCsv exporter) {
        this.service = service;
        this.usuarios = usuarios;
        this.produtos = produtos;
        this.exporter = exporter;
    }

    @FXML
    private void initialize() {
        tipoCombo.getItems().setAll(TipoRelatorio.values());
        tipoCombo.getSelectionModel().select(TipoRelatorio.VENDAS_POR_DIA);
        inicioPicker.setValue(LocalDate.now().minusMonths(1));
        fimPicker.setValue(LocalDate.now());
        operadorCombo.getItems().setAll(usuarios.listar());
        formaCombo.getItems().setAll(FormaPagamento.values());
        produtoCombo.getItems().setAll(produtos.listarAtivos());
        operadorCombo.setConverter(converter(Usuario::getNome));
        produtoCombo.setConverter(converter(Produto::getNome));
        categoriaColumn.setCellValueFactory(row ->
                new SimpleStringProperty(text(row.getValue().categoria())));
        detalheColumn.setCellValueFactory(row ->
                new SimpleStringProperty(text(row.getValue().detalhe())));
        quantidadeColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().quantidade() == null
                        ? "" : row.getValue().quantidade().toString()));
        valorColumn.setCellValueFactory(row ->
                new SimpleStringProperty(money(row.getValue().valor())));
        valor2Column.setCellValueFactory(row ->
                new SimpleStringProperty(money(row.getValue().valorSecundario())));
        dataColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().data() == null
                        ? "" : row.getValue().data().format(DATE_TIME)));
    }

    @FXML
    private void generate() {
        try {
            currentRows = service.gerar(tipoCombo.getValue(), filter());
            tabela.getItems().setAll(currentRows);
            message(currentRows.size() + " linha(s) gerada(s).", false);
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    @FXML
    private void exportCsv() {
        if (currentRows.isEmpty()) {
            message("Gere um relatório antes de exportar.", true);
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar relatório");
        chooser.setInitialFileName("relatorio-" + tipoCombo.getValue().name().toLowerCase()
                + ".csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivo CSV", "*.csv"));
        File file = chooser.showSaveDialog(tabela.getScene().getWindow());
        if (file == null) return;
        try {
            exporter.exportar(file.toPath(), tipoCombo.getValue(), currentRows);
            message("Relatório exportado para " + file.getAbsolutePath(), false);
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    @FXML
    private void clearFilters() {
        operadorCombo.getSelectionModel().clearSelection();
        formaCombo.getSelectionModel().clearSelection();
        produtoCombo.getSelectionModel().clearSelection();
        inicioPicker.setValue(LocalDate.now().minusMonths(1));
        fimPicker.setValue(LocalDate.now());
    }

    private FiltroRelatorio filter() {
        Usuario user = operadorCombo.getValue();
        Produto product = produtoCombo.getValue();
        return new FiltroRelatorio(
                inicioPicker.getValue(), fimPicker.getValue(),
                user == null ? null : user.getId(), formaCombo.getValue(),
                product == null ? null : product.getId());
    }

    private <T> StringConverter<T> converter(java.util.function.Function<T, String> label) {
        return new StringConverter<>() {
            @Override public String toString(T value) {
                return value == null ? "" : label.apply(value);
            }
            @Override public T fromString(String value) { return null; }
        };
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String money(java.math.BigDecimal value) {
        return value == null ? "" : CURRENCY.format(value);
    }

    private void message(String text, boolean error) {
        mensagemLabel.setText(text == null ? "Ocorreu um erro." : text);
        mensagemLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }
}
