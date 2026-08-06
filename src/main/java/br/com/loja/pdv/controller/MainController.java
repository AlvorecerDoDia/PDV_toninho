package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.service.CaixaService;
import br.com.loja.pdv.service.SessaoUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;

/** Controla uma navegacao unica, sem perfis ou menus condicionais. */
public final class MainController {
    @FXML private TabPane tabs;
    @FXML private Tab vendasTab;
    @FXML private Tab caixaTab;
    @FXML private Tab historicoTab;
    @FXML private Tab cadastroTab;
    @FXML private Tab estoqueTab;
    @FXML private Tab usuariosTab;
    @FXML private ToggleButton vendasNav;
    @FXML private ToggleButton caixaNav;
    @FXML private ToggleButton historicoNav;
    @FXML private ToggleButton cadastroNav;
    @FXML private ToggleButton estoqueNav;
    @FXML private ToggleButton usuariosNav;
    @FXML private Label usuarioLabel;
    @FXML private Label caixaLabel;

    private final SessaoUsuario sessao;
    private final CaixaService caixaService;

    public MainController(SessaoUsuario sessao, CaixaService caixaService) {
        this.sessao = sessao;
        this.caixaService = caixaService;
    }

    @FXML
    private void initialize() {
        Usuario usuario = sessao.exigirLogin();
        usuarioLabel.setText(usuario.getNome());
        tabs.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, atual) -> {
                    sincronizarNavegacao(atual);
                    atualizarIndicadorCaixa();
                });
        tabs.getSelectionModel().select(vendasTab);
        vendasNav.setSelected(true);
        atualizarIndicadorCaixa();
    }

    @FXML private void showVendas() { selecionar(vendasTab, vendasNav); }
    @FXML private void showCaixa() { selecionar(caixaTab, caixaNav); }
    @FXML private void showHistorico() { selecionar(historicoTab, historicoNav); }
    @FXML private void showCadastro() { selecionar(cadastroTab, cadastroNav); }
    @FXML private void showEstoque() { selecionar(estoqueTab, estoqueNav); }
    @FXML private void showUsuarios() { selecionar(usuariosTab, usuariosNav); }

    private void selecionar(Tab tab, ToggleButton botao) {
        tabs.getSelectionModel().select(tab);
        botao.setSelected(true);
    }

    private void sincronizarNavegacao(Tab tab) {
        if (tab == vendasTab) vendasNav.setSelected(true);
        else if (tab == caixaTab) caixaNav.setSelected(true);
        else if (tab == historicoTab) historicoNav.setSelected(true);
        else if (tab == cadastroTab) cadastroNav.setSelected(true);
        else if (tab == estoqueTab) estoqueNav.setSelected(true);
        else if (tab == usuariosTab) usuariosNav.setSelected(true);
    }

    private void atualizarIndicadorCaixa() {
        try {
            boolean aberto = caixaService.buscarCaixaAtual().isPresent();
            caixaLabel.setText(aberto ? "● Caixa aberto" : "● Caixa fechado");
            caixaLabel.getStyleClass().removeAll("cash-open", "cash-closed");
            caixaLabel.getStyleClass().add(aberto ? "cash-open" : "cash-closed");
        } catch (RuntimeException exception) {
            caixaLabel.setText("Caixa indisponível");
        }
    }
}
