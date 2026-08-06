package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.enums.FormaPagamento;
import br.com.loja.pdv.domain.model.CarrinhoVenda;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.domain.model.Venda;
import br.com.loja.pdv.service.PagamentoService;
import br.com.loja.pdv.service.VendaService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

/** Controla a unica forma de pagamento escolhida para a venda. */
public final class PagamentoController {
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    @FXML private ComboBox<FormaPagamento> formaCombo;
    @FXML private TextField valorField;
    @FXML private Label recebidoLabel;
    @FXML private Label trocoLabel;
    @FXML private Label mensagemLabel;

    private final PagamentoService pagamentos;
    private final VendaService vendas;
    private final CarrinhoVenda carrinho;
    private Consumer<String> onSaleFinalized;

    public PagamentoController(
            PagamentoService pagamentos, VendaService vendas, CarrinhoVenda carrinho) {
        this.pagamentos = pagamentos;
        this.vendas = vendas;
        this.carrinho = carrinho;
    }

    @FXML
    private void initialize() {
        UiFormatters.moeda(valorField);
        formaCombo.getItems().setAll(FormaPagamento.values());
        formaCombo.getSelectionModel().select(FormaPagamento.DINHEIRO);
        formaCombo.valueProperty().addListener(
                (observable, anterior, atual) -> atualizarForma());
        valorField.textProperty().addListener(
                (observable, anterior, atual) -> atualizarIndicadores());
        atualizarForma();
    }

    @FXML
    private void finishSale() {
        try {
            Pagamento pagamento = pagamentos.criar(
                    formaCombo.getValue(), valorInformado(), carrinho.getTotal());
            Venda venda = vendas.finalizar(carrinho, pagamento);
            valorField.clear();
            formaCombo.getSelectionModel().select(FormaPagamento.DINHEIRO);
            refreshTotals();
            if (onSaleFinalized != null) {
                onSaleFinalized.accept("Venda " + venda.getNumero()
                        + " finalizada. Troco: " + CURRENCY.format(venda.getTroco()));
            }
        } catch (RuntimeException exception) {
            mensagem(ErrorHandler.mensagem(exception), true);
        }
    }

    public void setOnSaleFinalized(Consumer<String> callback) {
        onSaleFinalized = callback;
    }

    public void focus() {
        if (valorField.isDisable()) formaCombo.requestFocus();
        else valorField.requestFocus();
    }

    public void refreshTotals() {
        atualizarForma();
        atualizarIndicadores();
    }

    private void atualizarForma() {
        boolean dinheiro = formaCombo.getValue() == FormaPagamento.DINHEIRO;
        valorField.setDisable(!dinheiro);
        if (!dinheiro) {
            valorField.setText(carrinho.getTotal().signum() > 0
                    ? carrinho.getTotal().toPlainString().replace('.', ',')
                    : "");
        }
        atualizarIndicadores();
    }

    private void atualizarIndicadores() {
        try {
            BigDecimal recebido = valorInformado();
            recebidoLabel.setText(CURRENCY.format(recebido));
            BigDecimal troco = BigDecimal.ZERO.setScale(2);
            if (formaCombo.getValue() == FormaPagamento.DINHEIRO
                    && recebido.compareTo(carrinho.getTotal()) >= 0) {
                troco = recebido.subtract(carrinho.getTotal());
            }
            trocoLabel.setText(CURRENCY.format(troco));
        } catch (RuntimeException exception) {
            recebidoLabel.setText(CURRENCY.format(BigDecimal.ZERO));
            trocoLabel.setText(CURRENCY.format(BigDecimal.ZERO));
        }
    }

    private BigDecimal valorInformado() {
        if (formaCombo.getValue() != FormaPagamento.DINHEIRO) {
            return carrinho.getTotal();
        }
        String texto = valorField.getText() == null ? "" : valorField.getText().strip();
        if (texto.isEmpty()) return BigDecimal.ZERO.setScale(2);
        if (texto.contains(",")) {
            texto = texto.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(texto);
    }

    private void mensagem(String texto, boolean erro) {
        mensagemLabel.setText(texto == null ? "Ocorreu um erro." : texto);
        mensagemLabel.setStyle(erro
                ? "-fx-text-fill: #b91c1c;"
                : "-fx-text-fill: #166534;");
    }
}
