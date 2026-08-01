package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.enums.FormaPagamento;
import br.com.loja.pdv.domain.model.CarrinhoVenda;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.domain.model.Venda;
import br.com.loja.pdv.service.PagamentoService;
import br.com.loja.pdv.service.VendaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Gerencia as formas de pagamento antes de solicitar a finalizacao da venda. */
public final class PagamentoController {
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    @FXML private ComboBox<FormaPagamento> formaCombo;
    @FXML private TextField valorField;
    @FXML private Label recebidoLabel;
    @FXML private Label restanteLabel;
    @FXML private Label trocoLabel;
    @FXML private Label mensagemLabel;
    @FXML private TableView<Pagamento> pagamentosTable;
    @FXML private TableColumn<Pagamento, String> formaColumn;
    @FXML private TableColumn<Pagamento, String> valorColumn;

    private final PagamentoService pagamentos;
    private final VendaService vendas;
    private final CarrinhoVenda carrinho;
    private final List<Pagamento> currentPayments = new ArrayList<>();
    private Consumer<String> onSaleFinalized;

    /** Recebe os servicos e objetos de sessao usados pelas acoes desta tela. */
    public PagamentoController(
            PagamentoService pagamentos, VendaService vendas, CarrinhoVenda carrinho) {
        this.pagamentos = pagamentos;
        this.vendas = vendas;
        this.carrinho = carrinho;
    }

    /** Configura formas, tabela e valores iniciais do painel de recebimento. */
    @FXML
    private void initialize() {
        UiFormatters.moeda(valorField);
        formaCombo.getItems().setAll(FormaPagamento.values());
        formaCombo.getSelectionModel().select(FormaPagamento.DINHEIRO);
        formaColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getForma().name()));
        valorColumn.setCellValueFactory(row ->
                new SimpleStringProperty(CURRENCY.format(row.getValue().getValor())));
        refreshTotals();
    }

    /** Adiciona uma parcela de pagamento e recalcula restante e troco. */
    @FXML
    private void addPayment() {
        try {
            currentPayments.add(pagamentos.criar(
                    formaCombo.getValue(), parseMoney(valorField.getText())));
            valorField.clear();
            pagamentosTable.getItems().setAll(currentPayments);
            refreshTotals();
            message("Pagamento adicionado.", false);
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    /** Remove a parcela selecionada e atualiza os totais. */
    @FXML
    private void removePayment() {
        Pagamento selected = pagamentosTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            message("Selecione um pagamento.", true);
            return;
        }
        currentPayments.remove(selected);
        pagamentosTable.getItems().setAll(currentPayments);
        refreshTotals();
        message("Pagamento removido.", false);
    }

    /** Valida os pagamentos, finaliza a venda e avisa a tela do carrinho. */
    @FXML
    private void finishSale() {
        try {
            Venda venda = vendas.finalizar(carrinho, List.copyOf(currentPayments));
            currentPayments.clear();
            pagamentosTable.getItems().clear();
            refreshTotals();
            if (onSaleFinalized != null) {
                onSaleFinalized.accept("Venda " + venda.getNumero() + " finalizada. Troco: "
                        + CURRENCY.format(venda.getTroco()));
            }
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    /** Registra a acao executada depois que uma venda for concluida. */
    public void setOnSaleFinalized(Consumer<String> callback) {
        onSaleFinalized = callback;
    }

    /** Move o foco para o campo de valor ao abrir a etapa de pagamento. */
    public void focus() {
        valorField.requestFocus();
    }

    /** Recalcula os indicadores de total recebido, restante e troco. */
    public void refreshTotals() {
        BigDecimal received = pagamentos.totalRecebido(currentPayments);
        BigDecimal remaining = carrinho.getTotal().subtract(received).max(BigDecimal.ZERO);
        BigDecimal change = BigDecimal.ZERO.setScale(2);
        if (received.compareTo(carrinho.getTotal()) >= 0) {
            BigDecimal possibleChange = received.subtract(carrinho.getTotal());
            BigDecimal cash = currentPayments.stream()
                    .filter(payment -> payment.getForma() == FormaPagamento.DINHEIRO)
                    .map(Pagamento::getValor)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            if (possibleChange.compareTo(cash) <= 0) {
                change = possibleChange;
            }
        }
        recebidoLabel.setText(CURRENCY.format(received));
        restanteLabel.setText(CURRENCY.format(remaining));
        trocoLabel.setText(CURRENCY.format(change));
    }

    /** Converte o texto monetario informado para BigDecimal. */
    private BigDecimal parseMoney(String text) {
        String normalized = text == null ? "" : text.strip();
        if (normalized.contains(",")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(normalized);
    }

    /** Mostra feedback relacionado apenas ao pagamento. */
    private void message(String text, boolean error) {
        mensagemLabel.setText(text == null ? "Ocorreu um erro." : text);
        mensagemLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }
}
