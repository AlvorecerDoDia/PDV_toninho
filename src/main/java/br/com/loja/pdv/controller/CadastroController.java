package br.com.loja.pdv.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Mantem produtos e categorias sincronizados dentro do modulo Cadastro. */
public final class CadastroController {
    @FXML private TabPane cadastroTabs;
    @FXML private Tab produtosTab;
    @FXML private ProdutoController produtoViewController;
    @FXML private CategoriaController categoriaViewController;

    /** Atualiza a tela escolhida sempre que o usuario troca a aba interna. */
    @FXML
    private void initialize() {
        cadastroTabs.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, atual) -> recarregar(atual));
        recarregar(cadastroTabs.getSelectionModel().getSelectedItem());
    }

    private void recarregar(Tab tab) {
        if (tab == produtosTab && produtoViewController != null) {
            produtoViewController.recarregar();
        } else if (categoriaViewController != null) {
            categoriaViewController.recarregar();
        }
    }
}
