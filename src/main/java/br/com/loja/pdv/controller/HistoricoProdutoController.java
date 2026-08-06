package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.model.Categoria;
import br.com.loja.pdv.domain.model.ProdutoVendidoHistorico;
import br.com.loja.pdv.service.CategoriaService;
import br.com.loja.pdv.service.VendaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Consulta os produtos vendidos por periodo e por varias categorias. */
public final class HistoricoProdutoController {
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private DatePicker inicioPicker;
    @FXML private DatePicker fimPicker;
    @FXML private MenuButton categoriaMenu;
    @FXML private TableView<ProdutoVendidoHistorico> tabela;
    @FXML private TableColumn<ProdutoVendidoHistorico, String> dataColumn;
    @FXML private TableColumn<ProdutoVendidoHistorico, String> vendaColumn;
    @FXML private TableColumn<ProdutoVendidoHistorico, String> produtoColumn;
    @FXML private TableColumn<ProdutoVendidoHistorico, String> categoriaColumn;
    @FXML private TableColumn<ProdutoVendidoHistorico, String> quantidadeColumn;
    @FXML private TableColumn<ProdutoVendidoHistorico, String> unitarioColumn;
    @FXML private TableColumn<ProdutoVendidoHistorico, String> subtotalColumn;
    @FXML private Label quantidadeTotalLabel;
    @FXML private Label valorTotalLabel;
    @FXML private Label mensagemLabel;

    private final VendaService vendas;
    private final CategoriaService categorias;
    private final Map<CheckBox, Categoria> opcoesCategorias = new LinkedHashMap<>();

    public HistoricoProdutoController(
            VendaService vendas, CategoriaService categorias) {
        this.vendas = vendas;
        this.categorias = categorias;
    }

    /** Configura colunas, periodo inicial e opcoes do filtro de categorias. */
    @FXML
    private void initialize() {
        dataColumn.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().dataVenda().format(DATE_TIME)));
        vendaColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().numeroVenda()));
        produtoColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().produtoNome()));
        categoriaColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().categoriaNome()));
        quantidadeColumn.setCellValueFactory(row -> new SimpleStringProperty(
                Integer.toString(row.getValue().quantidade())));
        unitarioColumn.setCellValueFactory(row -> new SimpleStringProperty(
                CURRENCY.format(row.getValue().precoUnitario())));
        subtotalColumn.setCellValueFactory(row -> new SimpleStringProperty(
                CURRENCY.format(row.getValue().subtotal())));

        LocalDate hoje = LocalDate.now();
        inicioPicker.setValue(hoje.withDayOfMonth(1));
        fimPicker.setValue(hoje);
        recarregarCategorias();
        search();
    }

    /** Executa a consulta usando o periodo e todas as categorias marcadas. */
    @FXML
    private void search() {
        try {
            var itens = vendas.listarProdutosVendidos(
                    inicioPicker.getValue(), fimPicker.getValue(), categoriasSelecionadas());
            tabela.getItems().setAll(itens);
            long quantidade = itens.stream()
                    .mapToLong(ProdutoVendidoHistorico::quantidade)
                    .sum();
            BigDecimal valor = itens.stream()
                    .map(ProdutoVendidoHistorico::subtotal)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            quantidadeTotalLabel.setText(Long.toString(quantidade));
            valorTotalLabel.setText(CURRENCY.format(valor));
            message(itens.isEmpty()
                    ? "Nenhum produto vendido foi encontrado para os filtros informados."
                    : itens.size() + " registro(s) encontrado(s).", false);
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    /** Restaura o mes atual e remove todas as categorias selecionadas. */
    @FXML
    private void clearFilters() {
        LocalDate hoje = LocalDate.now();
        inicioPicker.setValue(hoje.withDayOfMonth(1));
        fimPicker.setValue(hoje);
        opcoesCategorias.keySet().forEach(item -> item.setSelected(false));
        atualizarTextoCategorias();
        search();
    }

    /** Recria o menu para incluir categorias novas e categorias inativas. */
    private void recarregarCategorias() {
        opcoesCategorias.clear();
        categoriaMenu.getItems().clear();
        for (Categoria categoria : categorias.listarTodas()) {
            CheckBox selecao = new CheckBox(categoria.getNome());
            selecao.setOnAction(event -> atualizarTextoCategorias());
            CustomMenuItem item = new CustomMenuItem(selecao);
            item.setHideOnClick(false);
            opcoesCategorias.put(selecao, categoria);
            categoriaMenu.getItems().add(item);
        }
        atualizarTextoCategorias();
    }

    private Set<Long> categoriasSelecionadas() {
        return opcoesCategorias.entrySet().stream()
                .filter(entry -> entry.getKey().isSelected())
                .map(entry -> entry.getValue().getId())
                .collect(Collectors.toUnmodifiableSet());
    }

    private void atualizarTextoCategorias() {
        long selecionadas = opcoesCategorias.keySet().stream()
                .filter(CheckBox::isSelected)
                .count();
        categoriaMenu.setText(selecionadas == 0
                ? "Todas as categorias"
                : selecionadas == 1
                        ? "1 categoria selecionada"
                        : selecionadas + " categorias selecionadas");
    }

    private void message(String text, boolean error) {
        mensagemLabel.setText(text == null ? "Ocorreu um erro." : text);
        mensagemLabel.setStyle(error
                ? "-fx-text-fill: #b91c1c;"
                : "-fx-text-fill: #166534;");
    }
}
