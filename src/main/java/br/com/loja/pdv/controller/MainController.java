package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.enums.Permissao;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.service.SessaoUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public final class MainController {
    @FXML private TabPane tabs;
    @FXML private Tab produtosTab;
    @FXML private Tab estoqueTab;
    @FXML private Tab caixaTab;
    @FXML private Tab usuariosTab;
    @FXML private Label usuarioLabel;
    private final SessaoUsuario sessao;

    public MainController(SessaoUsuario sessao) {
        this.sessao = sessao;
    }

    @FXML
    private void initialize() {
        Usuario usuario = sessao.atual().orElseThrow();
        usuarioLabel.setText(usuario.getNome() + " — " + usuario.getPerfil());
        if (!usuario.getPerfil().permite(Permissao.PRODUTOS)) tabs.getTabs().remove(produtosTab);
        if (!usuario.getPerfil().permite(Permissao.ESTOQUE)) tabs.getTabs().remove(estoqueTab);
        if (!usuario.getPerfil().permite(Permissao.CAIXA)) tabs.getTabs().remove(caixaTab);
        if (!usuario.getPerfil().permite(Permissao.USUARIOS)) tabs.getTabs().remove(usuariosTab);
    }
}
