package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.service.ProdutoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;

/** Controla o formulario e a tabela do cadastro de produtos. */
public final class ProdutoController {
    // Componentes declarados no FXML e preenchidos automaticamente pelo JavaFX.
    @FXML private TextField codigoField;
    @FXML private TextField nomeField;
    @FXML private TextField custoField;
    @FXML private TextField vendaField;
    @FXML private TextField quantidadeField;
    @FXML private TextField minimoField;
    @FXML private TextField pesquisaField;
    @FXML private TableView<Produto> tabela;
    @FXML private TableColumn<Produto, String> nomeColumn;
    @FXML private TableColumn<Produto, String> codigoColumn;
    @FXML private TableColumn<Produto, String> vendaColumn;
    @FXML private TableColumn<Produto, String> estoqueColumn;
    @FXML private TableColumn<Produto, String> statusColumn;
    @FXML private Label mensagemLabel;
    @FXML private Button desativarButton;
    @FXML private Button reativarButton;

    // O servico concentra regras; selected define se o formulario cria ou edita.
    private final ProdutoService service;
    private Produto selected;

    /** Recebe os servicos e objetos de sessao usados pelas acoes desta tela. */
    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    /** Configura campos, tabela, selecao e pesquisa do cadastro de produtos. */
    @FXML
    private void initialize() {
        UiFormatters.moeda(custoField, vendaField);
        UiFormatters.inteiro(quantidadeField, minimoField);
        nomeColumn.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getNome()));
        codigoColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getCodigoBarras() == null ? "" : row.getValue().getCodigoBarras()));
        vendaColumn.setCellValueFactory(row -> new SimpleStringProperty(
                "R$ " + row.getValue().getPrecoVenda().toPlainString().replace('.', ',')));
        estoqueColumn.setCellValueFactory(row -> new SimpleStringProperty(
                Integer.toString(row.getValue().getQuantidadeEstoque())));
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

    /** Cria ou atualiza um produto conforme a existencia de uma selecao atual. */
    @FXML
    private void save() {
        try {
            Produto produto = selected == null ? new Produto() : selected;
            produto.setCodigoBarras(codigoField.getText());
            produto.setNome(nomeField.getText());
            produto.setPrecoCusto(new BigDecimal(custoField.getText().replace(',', '.')));
            produto.setPrecoVenda(new BigDecimal(vendaField.getText().replace(',', '.')));
            produto.setQuantidadeEstoque(parseInteger(quantidadeField, "quantidade inicial"));
            produto.setEstoqueMinimo(parseInteger(minimoField, "estoque minimo"));
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

    /** Ativa ou desativa o produto selecionado com confirmacao quando necessario. */
    private void changeStatus(boolean active) {
        if (selected == null) return;
        if (!active && !confirm(
                "Desativar produto",
                "O produto deixará de aparecer nas vendas. Deseja continuar?")) return;
        try {
            if (active) service.reativar(selected.getId()); else service.desativar(selected.getId());
            clear();
            refresh();
            message(active ? "Produto reativado." : "Produto desativado.", false);
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    /** Volta o formulario ao modo de novo cadastro. */
    @FXML
    private void clear() {
        selected = null;
        tabela.getSelectionModel().clearSelection();
        codigoField.clear(); nomeField.clear(); custoField.clear(); vendaField.clear();
        quantidadeField.setText("0");
        quantidadeField.setDisable(false);
        minimoField.setText("0");
        desativarButton.setDisable(true);
        reativarButton.setDisable(true);
    }

    /** Preenche o formulario com o produto escolhido e bloqueia a quantidade em edicoes. */
    private void select(Produto produto) {
        selected = produto;
        if (produto == null) return;
        codigoField.setText(produto.getCodigoBarras());
        nomeField.setText(produto.getNome());
        custoField.setText(produto.getPrecoCusto().toPlainString());
        vendaField.setText(produto.getPrecoVenda().toPlainString());
        quantidadeField.setText(Integer.toString(produto.getQuantidadeEstoque()));
        quantidadeField.setDisable(true);
        minimoField.setText(Integer.toString(produto.getEstoqueMinimo()));
        desativarButton.setDisable(!produto.isAtivo());
        reativarButton.setDisable(produto.isAtivo());
    }

    /** Converte a quantidade para inteiro e gera uma validacao amigavel em caso de erro. */
    private int parseInteger(TextField field, String name) {
        if (field.getText() == null || field.getText().isBlank()) {
            throw new NumberFormatException("Informe " + name + ".");
        }
        return Integer.parseInt(field.getText());
    }

    /** Executa a pesquisa atual e substitui os itens exibidos na tabela. */
    private void refresh() {
        tabela.getItems().setAll(service.pesquisar(pesquisaField.getText()));
    }

    /** Apresenta o resultado da operacao dentro do cartao de cadastro. */
    private void message(String text, boolean error) {
        mensagemLabel.setText(text == null ? "Ocorreu um erro." : text);
        mensagemLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }

    /** Solicita confirmacao antes de retirar um produto do fluxo de vendas. */
    private boolean confirm(String title, String text) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
