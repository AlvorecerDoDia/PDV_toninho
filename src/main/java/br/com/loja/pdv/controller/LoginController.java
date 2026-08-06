package br.com.loja.pdv.controller;

import br.com.loja.pdv.service.AutenticacaoService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** Autentica o usuario e abre a tela principal. */
public final class LoginController {
    @FXML private TextField loginField;
    @FXML private PasswordField senhaField;
    @FXML private Label mensagemLabel;

    private final AutenticacaoService autenticacao;
    private final Runnable onSuccess;

    public LoginController(AutenticacaoService autenticacao, Runnable onSuccess) {
        this.autenticacao = autenticacao;
        this.onSuccess = onSuccess;
    }

    @FXML
    private void login() {
        try {
            autenticacao.autenticar(loginField.getText(), senhaField.getText().toCharArray());
            onSuccess.run();
        } catch (RuntimeException exception) {
            mensagemLabel.setText(ErrorHandler.mensagem(exception));
        }
    }
}
