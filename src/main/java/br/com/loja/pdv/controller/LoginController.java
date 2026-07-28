package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.service.AutenticacaoService;
import br.com.loja.pdv.service.UsuarioService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class LoginController {
    @FXML private TextField loginField;
    @FXML private PasswordField senhaField;
    @FXML private PasswordField novaSenhaField;
    @FXML private Label novaSenhaLabel;
    @FXML private Label mensagemLabel;
    private final AutenticacaoService autenticacao;
    private final UsuarioService usuarios;
    private final Runnable onSuccess;

    public LoginController(
            AutenticacaoService autenticacao, UsuarioService usuarios, Runnable onSuccess) {
        this.autenticacao = autenticacao;
        this.usuarios = usuarios;
        this.onSuccess = onSuccess;
    }

    @FXML
    private void login() {
        try {
            Usuario usuario = autenticacao.autenticar(
                    loginField.getText(), senhaField.getText().toCharArray());
            if (usuario.isAlterarSenha()) {
                if (!novaSenhaField.isVisible()) {
                    autenticacao.sair();
                    novaSenhaField.setVisible(true);
                    novaSenhaField.setManaged(true);
                    novaSenhaLabel.setVisible(true);
                    novaSenhaLabel.setManaged(true);
                    mensagemLabel.setText("Defina uma nova senha para continuar.");
                    return;
                }
                usuarios.trocarSenha(usuario.getId(), novaSenhaField.getText().toCharArray());
                autenticacao.autenticar(
                        loginField.getText(), novaSenhaField.getText().toCharArray());
            }
            onSuccess.run();
        } catch (RuntimeException exception) {
            mensagemLabel.setText(exception.getMessage());
        }
    }
}
