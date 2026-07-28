package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.model.ItemVenda;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.domain.model.Venda;
import br.com.loja.pdv.repository.PagamentoRepository;
import br.com.loja.pdv.service.UsuarioService;
import br.com.loja.pdv.service.VendaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class HistoricoVendaController {
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TextField numeroField;
    @FXML private DatePicker inicioPicker;
    @FXML private DatePicker fimPicker;
    @FXML private ComboBox<Usuario> operadorCombo;
    @FXML private TextField motivoField;
    @FXML private Label mensagemLabel;
    @FXML private TextArea itensArea;
    @FXML private TextArea pagamentosArea;
    @FXML private TableView<Venda> vendasTable;
    @FXML private TableColumn<Venda, String> numeroColumn;
    @FXML private TableColumn<Venda, String> dataColumn;
    @FXML private TableColumn<Venda, String> operadorColumn;
    @FXML private TableColumn<Venda, String> totalColumn;
    @FXML private TableColumn<Venda, String> statusColumn;

    private final VendaService vendas;
    private final UsuarioService usuarios;
    private final PagamentoRepository pagamentos;

    public HistoricoVendaController(
            VendaService vendas, UsuarioService usuarios, PagamentoRepository pagamentos) {
        this.vendas = vendas;
        this.usuarios = usuarios;
        this.pagamentos = pagamentos;
    }

    @FXML
    private void initialize() {
        inicioPicker.setValue(LocalDate.now().minusMonths(1));
        fimPicker.setValue(LocalDate.now());
        operadorCombo.getItems().setAll(usuarios.listar());
        operadorCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Usuario usuario) {
                return usuario == null ? "Todos" : usuario.getNome();
            }
            @Override public Usuario fromString(String text) { return null; }
        });
        numeroColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getNumero()));
        dataColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getCriadoEm().format(DATE_TIME)));
        operadorColumn.setCellValueFactory(row ->
                new SimpleStringProperty(Long.toString(row.getValue().getOperadorId())));
        totalColumn.setCellValueFactory(row ->
                new SimpleStringProperty(CURRENCY.format(row.getValue().getTotal())));
        statusColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getStatus().name()));
        vendasTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, value) -> showDetails(value));
        clearDetails();
    }

    @FXML
    private void search() {
        try {
            List<Venda> result;
            if (numeroField.getText() != null && !numeroField.getText().isBlank()) {
                result = List.of(vendas.buscarPorNumero(numeroField.getText()));
            } else {
                Usuario operator = operadorCombo.getValue();
                result = vendas.listar(
                        inicioPicker.getValue(), fimPicker.getValue(),
                        operator == null ? null : operator.getId());
            }
            vendasTable.getItems().setAll(result);
            if (!result.isEmpty()) vendasTable.getSelectionModel().selectFirst();
            else clearDetails();
            message(result.size() + " venda(s) encontrada(s).", false);
        } catch (RuntimeException exception) {
            message(exception.getMessage(), true);
        }
    }

    @FXML
    private void clearFilters() {
        numeroField.clear();
        operadorCombo.getSelectionModel().clearSelection();
        inicioPicker.setValue(LocalDate.now().minusMonths(1));
        fimPicker.setValue(LocalDate.now());
        search();
    }

    @FXML
    private void cancelSelected() {
        Venda selected = vendasTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            message("Selecione uma venda.", true);
            return;
        }
        try {
            vendas.cancelar(selected.getId(), motivoField.getText());
            motivoField.clear();
            search();
            message("Venda cancelada e valores estornados.", false);
        } catch (RuntimeException exception) {
            message(exception.getMessage(), true);
        }
    }

    private void showDetails(Venda selected) {
        if (selected == null) {
            clearDetails();
            return;
        }
        try {
            Venda detailed = vendas.detalhar(selected.getId());
            List<Pagamento> salePayments = pagamentos.listarPorVenda(selected.getId());
            itensArea.setText(detailed.getItens().stream()
                    .map(this::formatItem)
                    .reduce((left, right) -> left + System.lineSeparator() + right)
                    .orElse(""));
            pagamentosArea.setText(salePayments.stream()
                    .map(payment -> payment.getForma() + " — "
                            + CURRENCY.format(payment.getValor()))
                    .reduce((left, right) -> left + System.lineSeparator() + right)
                    .orElse(""));
        } catch (RuntimeException exception) {
            message(exception.getMessage(), true);
        }
    }

    private String formatItem(ItemVenda item) {
        return item.getQuantidade() + " x " + item.getProdutoNome()
                + " — " + CURRENCY.format(item.getSubtotal());
    }

    private void clearDetails() {
        itensArea.clear();
        pagamentosArea.clear();
    }

    private void message(String text, boolean error) {
        mensagemLabel.setText(text == null ? "Ocorreu um erro." : text);
        mensagemLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }
}
