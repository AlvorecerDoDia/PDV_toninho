package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.service.UsuarioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/** Controla o cadastro simples de usuarios. */
public final class UsuarioController {
    @FXML private TextField nomeField;
    @FXML private TextField loginField;
    @FXML private PasswordField senhaField;
    @FXML private CheckBox ativoCheck;
    @FXML private Label mensagemLabel;
    @FXML private TableView<Usuario> tabela;
    @FXML private TableColumn<Usuario, String> nomeColumn;
    @FXML private TableColumn<Usuario, String> loginColumn;
    @FXML private TableColumn<Usuario, String> statusColumn;

    private final UsuarioService service;
    private Usuario selecionado;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @FXML
    private void initialize() {
        nomeColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getNome()));
        loginColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getLogin()));
        statusColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().isAtivo() ? "Ativo" : "Inativo"));
        tabela.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, atual) -> selecionar(atual));
        recarregar();
    }

    @FXML
    private void save() {
        try {
            if (selecionado == null) {
                service.criar(nomeField.getText(), loginField.getText(),
                        senhaField.getText().toCharArray());
            } else {
                service.atualizar(selecionado.getId(), nomeField.getText(),
                        loginField.getText(), ativoCheck.isSelected());
                if (!senhaField.getText().isBlank()) {
                    service.trocarSenha(
                            selecionado.getId(), senhaField.getText().toCharArray());
                }
            }
            clear();
            recarregar();
            mensagemLabel.setText("Usuário salvo.");
        } catch (RuntimeException exception) {
            mensagemLabel.setText(ErrorHandler.mensagem(exception));
        }
    }

    @FXML
    private void clear() {
        selecionado = null;
        tabela.getSelectionModel().clearSelection();
        nomeField.clear();
        loginField.clear();
        senhaField.clear();
        ativoCheck.setSelected(true);
    }

    private void recarregar() {
        tabela.getItems().setAll(service.listar());
    }

    private void selecionar(Usuario usuario) {
        selecionado = usuario;
        if (usuario == null) return;
        nomeField.setText(usuario.getNome());
        loginField.setText(usuario.getLogin());
        ativoCheck.setSelected(usuario.isAtivo());
        senhaField.clear();
    }
}
