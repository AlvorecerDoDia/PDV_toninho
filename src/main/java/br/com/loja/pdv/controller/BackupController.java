package br.com.loja.pdv.controller;

import br.com.loja.pdv.service.BackupService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Apresenta criacao, listagem e restauracao de backups ao administrador. */
public final class BackupController {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    @FXML private Label mensagemLabel;
    @FXML private TableView<Path> tabela;
    @FXML private TableColumn<Path, String> arquivoColumn;
    @FXML private TableColumn<Path, String> dataColumn;
    private final BackupService service;

    /** Recebe os servicos e objetos de sessao usados pelas acoes desta tela. */
    public BackupController(BackupService service) {
        this.service = service;
    }

    /** Prepara as colunas da tabela e carrega a lista inicial de backups. */
    @FXML
    private void initialize() {
        arquivoColumn.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getFileName().toString()));
        dataColumn.setCellValueFactory(row -> {
            try {
                Instant instant = java.nio.file.Files.getLastModifiedTime(row.getValue()).toInstant();
                return new SimpleStringProperty(DATE_TIME.format(
                        instant.atZone(ZoneId.systemDefault())));
            } catch (java.io.IOException exception) {
                return new SimpleStringProperty("Data indisponível");
            }
        });
        refresh();
    }

    /** Solicita um backup manual e informa o arquivo criado ao usuario. */
    @FXML
    private void createBackup() {
        try {
            Path created = service.criarManual();
            refresh();
            message("Backup criado: " + created, false);
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    /** Valida a selecao da tabela antes de iniciar uma restauracao. */
    @FXML
    private void restoreSelected() {
        Path selected = tabela.getSelectionModel().getSelectedItem();
        if (selected == null) {
            message("Selecione um backup.", true);
            return;
        }
        restore(selected);
    }

    /** Abre o seletor de arquivos para restaurar um banco externo. */
    @FXML
    private void chooseAndRestore() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar backup");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Banco SQLite", "*.db"));
        File file = chooser.showOpenDialog(tabela.getScene().getWindow());
        if (file != null) restore(file.toPath());
    }

    /** Recarrega a tabela a partir dos arquivos existentes na pasta de backups. */
    @FXML
    private void refresh() {
        try {
            tabela.getItems().setAll(service.listar());
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    /** Confirma a operacao destrutiva e delega a restauracao ao servico. */
    private void restore(Path file) {
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Restaurar este backup? Uma cópia do banco atual será criada antes.",
                ButtonType.YES, ButtonType.NO);
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            Path safety = service.restaurar(file);
            refresh();
            message("Backup restaurado. Cópia anterior: " + safety
                    + ". Reinicie o sistema para recarregar todas as telas.", false);
        } catch (RuntimeException exception) {
            message(ErrorHandler.mensagem(exception), true);
        }
    }

    /** Mostra feedback de sucesso ou erro sem abrir uma janela adicional. */
    private void message(String text, boolean error) {
        mensagemLabel.setText(text == null ? "Ocorreu um erro." : text);
        mensagemLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }
}
