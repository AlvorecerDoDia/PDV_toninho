package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.enums.Permissao;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.service.CaixaService;
import br.com.loja.pdv.service.SessaoUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public final class MainController {
    @FXML private TabPane tabs;
    @FXML private Tab vendasTab;
    @FXML private Tab historicoTab;
    @FXML private Tab relatoriosTab;
    @FXML private Tab backupTab;
    @FXML private Tab produtosTab;
    @FXML private Tab estoqueTab;
    @FXML private Tab caixaTab;
    @FXML private Tab usuariosTab;
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
        Usuario usuario = sessao.atual().orElseThrow();
        usuarioLabel.setText(usuario.getNome() + " — " + usuario.getPerfil());
        atualizarIndicadorCaixa();
        tabs.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, atual) -> atualizarIndicadorCaixa());
        if (!usuario.getPerfil().permite(Permissao.VENDAS)) tabs.getTabs().remove(vendasTab);
        if (!usuario.getPerfil().permite(Permissao.RELATORIOS)) {
            tabs.getTabs().remove(historicoTab);
            tabs.getTabs().remove(relatoriosTab);
        }
        if (!usuario.getPerfil().permite(Permissao.BACKUP)) tabs.getTabs().remove(backupTab);
        if (!usuario.getPerfil().permite(Permissao.PRODUTOS)) tabs.getTabs().remove(produtosTab);
        if (!usuario.getPerfil().permite(Permissao.ESTOQUE)) tabs.getTabs().remove(estoqueTab);
        if (!usuario.getPerfil().permite(Permissao.CAIXA)) tabs.getTabs().remove(caixaTab);
        if (!usuario.getPerfil().permite(Permissao.USUARIOS)) tabs.getTabs().remove(usuariosTab);
    }

    private void atualizarIndicadorCaixa() {
        try {
            boolean aberto = caixaService.buscarCaixaAtual().isPresent();
            caixaLabel.setText(aberto ? "● Caixa aberto" : "● Caixa fechado");
            caixaLabel.getStyleClass().removeAll("cash-open", "cash-closed");
            caixaLabel.getStyleClass().add(aberto ? "cash-open" : "cash-closed");
        } catch (RuntimeException exception) {
            caixaLabel.setText("Caixa indisponível");
            ErrorHandler.mensagem(exception);
        }
    }
}
