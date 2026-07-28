package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.enums.PerfilUsuario;
import br.com.loja.pdv.domain.enums.Permissao;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.service.SessaoUsuario;
import br.com.loja.pdv.service.UsuarioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class UsuarioController {
    @FXML private TextField nomeField;
    @FXML private TextField loginField;
    @FXML private PasswordField senhaField;
    @FXML private ComboBox<PerfilUsuario> perfilCombo;
    @FXML private CheckBox ativoCheck;
    @FXML private Label mensagemLabel;
    @FXML private TableView<Usuario> tabela;
    @FXML private TableColumn<Usuario, String> nomeColumn;
    @FXML private TableColumn<Usuario, String> loginColumn;
    @FXML private TableColumn<Usuario, String> perfilColumn;
    @FXML private TableColumn<Usuario, String> statusColumn;
    private final UsuarioService service;
    private final SessaoUsuario sessao;
    private Usuario selected;

    public UsuarioController(UsuarioService service, SessaoUsuario sessao) {
        this.service = service;
        this.sessao = sessao;
    }

    @FXML
    private void initialize() {
        perfilCombo.getItems().setAll(PerfilUsuario.values());
        nomeColumn.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getNome()));
        loginColumn.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getLogin()));
        perfilColumn.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getPerfil().name()));
        statusColumn.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().isAtivo() ? "Ativo" : "Inativo"));
        tabela.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, value) -> select(value));
        refresh();
    }

    @FXML
    private void save() {
        try {
            sessao.exigir(Permissao.USUARIOS);
            if (selected == null) {
                service.criar(nomeField.getText(), loginField.getText(),
                        senhaField.getText().toCharArray(), perfilCombo.getValue(), true);
            } else {
                service.atualizar(selected.getId(), nomeField.getText(), loginField.getText(),
                        perfilCombo.getValue(), ativoCheck.isSelected());
                if (!senhaField.getText().isBlank()) {
                    service.trocarSenha(selected.getId(), senhaField.getText().toCharArray());
                }
            }
            clear();
            refresh();
            mensagemLabel.setText("Usuário salvo.");
        } catch (RuntimeException exception) {
            mensagemLabel.setText(exception.getMessage());
        }
    }

    @FXML private void clear() {
        selected = null;
        tabela.getSelectionModel().clearSelection();
        nomeField.clear(); loginField.clear(); senhaField.clear();
        perfilCombo.setValue(PerfilUsuario.OPERADOR);
        ativoCheck.setSelected(true);
    }
    private void refresh() { tabela.getItems().setAll(service.listar()); }
    private void select(Usuario usuario) {
        selected = usuario;
        if (usuario == null) return;
        nomeField.setText(usuario.getNome());
        loginField.setText(usuario.getLogin());
        perfilCombo.setValue(usuario.getPerfil());
        ativoCheck.setSelected(usuario.isAtivo());
        senhaField.clear();
    }
}
