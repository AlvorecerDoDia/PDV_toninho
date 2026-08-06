package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.model.CarrinhoVenda;
import br.com.loja.pdv.domain.model.ItemCarrinho;
import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.service.ProdutoService;
import br.com.loja.pdv.service.SessaoUsuario;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Mantem a interacao rapida com o carrinho e os atalhos da tela de venda. */
public final class VendaController {
    // Formatador unico para manter valores monetarios consistentes na tela.
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    @FXML private VBox root;
    @FXML private TextField codigoField;
    @FXML private TextField pesquisaField;
    @FXML private ComboBox<Produto> resultadoCombo;
    @FXML private TextField quantidadeField;
    @FXML private TextField descontoField;
    @FXML private Label subtotalLabel;
    @FXML private Label descontoLabel;
    @FXML private Label totalLabel;
    @FXML private Label mensagemLabel;
    @FXML private TableView<ItemCarrinho> itensTable;
    @FXML private TableColumn<ItemCarrinho, String> produtoColumn;
    @FXML private TableColumn<ItemCarrinho, String> quantidadeColumn;
    @FXML private TableColumn<ItemCarrinho, String> unitarioColumn;
    @FXML private TableColumn<ItemCarrinho, String> subtotalColumn;
    @FXML private PagamentoController pagamentoPaneController;

    private final ProdutoService produtos;
    private final SessaoUsuario sessao;
    private final CarrinhoVenda carrinho;

    /** Recebe os servicos e objetos de sessao usados pelas acoes desta tela. */
    public VendaController(ProdutoService produtos, SessaoUsuario sessao) {
        this(produtos, sessao, new CarrinhoVenda());
    }

    /** Recebe os servicos e objetos de sessao usados pelas acoes desta tela. */
    public VendaController(
            ProdutoService produtos, SessaoUsuario sessao, CarrinhoVenda carrinho) {
        this.produtos = produtos;
        this.sessao = sessao;
        this.carrinho = carrinho;
    }

    /** Configura tabela, busca, atalhos e integracao com o painel de pagamentos. */
    @FXML
    private void initialize() {
        UiFormatters.inteiro(quantidadeField);
        UiFormatters.moeda(descontoField);
        sessao.exigirLogin();
        produtoColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getProduto().getNome()));
        quantidadeColumn.setCellValueFactory(row ->
                new SimpleStringProperty(Integer.toString(row.getValue().getQuantidade())));
        unitarioColumn.setCellValueFactory(row ->
                new SimpleStringProperty(CURRENCY.format(row.getValue().getPrecoUnitario())));
        subtotalColumn.setCellValueFactory(row ->
                new SimpleStringProperty(CURRENCY.format(row.getValue().getSubtotal())));
        resultadoCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Produto produto) {
                if (produto == null) return "";
                return produto.getNome() + (produto.isAtivo() ? "" : " (inativo)");
            }
            @Override public Produto fromString(String text) { return null; }
        });
        pesquisaField.textProperty().addListener((observable, oldValue, value) -> search());
        itensTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, value) -> {
                    if (value != null) {
                        quantidadeField.setText(Integer.toString(value.getQuantidade()));
                    }
                });
        installShortcuts();
        pagamentoPaneController.setOnSaleFinalized(this::saleFinalized);
        refresh();
        Platform.runLater(codigoField::requestFocus);
    }

    /** Localiza um produto pelo codigo e adiciona a quantidade padrao ao carrinho. */
    @FXML
    private void addByBarcode() {
        execute(() -> {
            Produto produto = produtos.buscarPorCodigoBarras(codigoField.getText())
                    .orElseThrow(() -> new ValidationException("Produto não encontrado."));
            carrinho.adicionar(produto, 1);
            codigoField.clear();
            codigoField.requestFocus();
            return "Produto adicionado.";
        });
    }

    /** Atualiza as sugestoes de produto conforme o texto digitado. */
    @FXML
    private void search() {
        try {
            resultadoCombo.getItems().setAll(produtos.pesquisar(pesquisaField.getText()));
            if (!resultadoCombo.getItems().isEmpty()) {
                resultadoCombo.getSelectionModel().selectFirst();
            }
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    /** Adiciona ao carrinho o produto escolhido na lista de resultados. */
    @FXML
    private void addSelected() {
        execute(() -> {
            Produto produto = resultadoCombo.getValue();
            if (produto == null) throw new ValidationException("Selecione um produto.");
            carrinho.adicionar(produto, 1);
            return "Produto adicionado.";
        });
    }

    /** Altera a quantidade do item selecionado respeitando o estoque disponivel. */
    @FXML
    private void changeQuantity() {
        execute(() -> {
            ItemCarrinho item = selectedItem();
            carrinho.alterarQuantidade(
                    item.getProduto().getId(), Integer.parseInt(quantidadeField.getText()));
            return "Quantidade alterada.";
        });
    }

    /** Remove do carrinho o item atualmente selecionado. */
    @FXML
    private void removeSelected() {
        execute(() -> {
            ItemCarrinho item = selectedItem();
            carrinho.remover(item.getProduto().getId());
            quantidadeField.clear();
            return "Item removido.";
        });
    }

    /** Confirma e remove todos os itens e descontos da venda em andamento. */
    @FXML
    private void clearCart() {
        carrinho.limpar();
        refresh();
        message("Carrinho limpo.", false);
        codigoField.requestFocus();
    }

    /** Converte e aplica o desconto informado ao carrinho. */
    @FXML
    private void applyDiscount() {
        execute(() -> {
            carrinho.aplicarDesconto(parseMoney(descontoField.getText()));
            return "Desconto aplicado.";
        });
    }

    /** Registra atalhos de teclado para acelerar a operacao no caixa. */
    private void installShortcuts() {
        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F2) pesquisaField.requestFocus();
            else if (event.getCode() == KeyCode.F4) quantidadeField.requestFocus();
            else if (event.getCode() == KeyCode.DELETE) removeSelected();
            else if (event.getCode() == KeyCode.F6) descontoField.requestFocus();
            else if (event.getCode() == KeyCode.F10) pagamentoPaneController.focus();
            else if (event.getCode() == KeyCode.ESCAPE) cancelCurrentInput();
            else return;
            event.consume();
        });
    }

    /** Limpa a entrada que esta em foco sem apagar a venda inteira. */
    private void cancelCurrentInput() {
        pesquisaField.clear();
        quantidadeField.clear();
        descontoField.clear();
        resultadoCombo.getItems().clear();
        codigoField.requestFocus();
        message("", false);
    }

    /** Retorna o item selecionado ou gera uma validacao clara. */
    private ItemCarrinho selectedItem() {
        ItemCarrinho item = itensTable.getSelectionModel().getSelectedItem();
        if (item == null) throw new ValidationException("Selecione um item do carrinho.");
        return item;
    }

    /** Executa uma acao do carrinho, atualiza a tela e trata erros de validacao. */
    private void execute(Action action) {
        try {
            message(action.run(), false);
            refresh();
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    /** Sincroniza tabela, totais e painel de pagamento com o estado do carrinho. */
    private void refresh() {
        itensTable.getItems().setAll(carrinho.getItens());
        subtotalLabel.setText(CURRENCY.format(carrinho.getSubtotal()));
        descontoLabel.setText(CURRENCY.format(carrinho.getDesconto()));
        totalLabel.setText(CURRENCY.format(carrinho.getTotal()));
        pagamentoPaneController.refreshTotals();
    }

    /** Limpa o fluxo visual e mostra o numero da venda concluida. */
    private void saleFinalized(String message) {
        refresh();
        message(message, false);
        codigoField.requestFocus();
    }

    /** Converte o desconto digitado para BigDecimal. */
    private BigDecimal parseMoney(String text) {
        String normalized = text == null ? "" : text.strip();
        if (normalized.contains(",")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(normalized);
    }

    /** Mostra feedback operacional sem interromper a leitura do codigo de barras. */
    private void message(String text, boolean error) {
        mensagemLabel.setText(text == null ? "Ocorreu um erro." : text);
        mensagemLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }

    /** Representa uma alteracao do carrinho que devolve feedback para a tela. */
    @FunctionalInterface
    private interface Action {
        /** Executa a alteracao solicitada. */
        String run();
    }
}
