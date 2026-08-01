package br.com.loja.pdv.controller;

import br.com.loja.pdv.domain.enums.Permissao;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.service.CaixaService;
import br.com.loja.pdv.service.SessaoUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;

/** Monta a navegacao permitida pelo perfil e exibe o estado da sessao. */
public final class MainController {
    // Abas reais ficam ocultas; o usuario navega pelos botoes laterais.
    @FXML private TabPane tabs;
    @FXML private Tab vendasTab;
    @FXML private Tab historicoTab;
    @FXML private Tab relatoriosTab;
    @FXML private Tab backupTab;
    @FXML private Tab produtosTab;
    @FXML private Tab estoqueTab;
    @FXML private Tab caixaTab;
    @FXML private Tab usuariosTab;
    @FXML private ToggleButton vendasNav;
    @FXML private ToggleButton historicoNav;
    @FXML private ToggleButton relatoriosNav;
    @FXML private ToggleButton backupNav;
    @FXML private ToggleButton produtosNav;
    @FXML private ToggleButton estoqueNav;
    @FXML private ToggleButton caixaNav;
    @FXML private ToggleButton usuariosNav;
    @FXML private Label usuarioLabel;
    @FXML private Label caixaLabel;
    @FXML private Label consultasSection;
    @FXML private Label gestaoSection;
    @FXML private Label sistemaSection;

    private final SessaoUsuario sessao;
    private final CaixaService caixaService;

    /** Recebe os servicos e objetos de sessao usados pelas acoes desta tela. */
    public MainController(SessaoUsuario sessao, CaixaService caixaService) {
        this.sessao = sessao;
        this.caixaService = caixaService;
    }

    /** Monta a navegacao conforme o perfil, seleciona a primeira area permitida e atualiza o caixa. */
    @FXML
    private void initialize() {
        Usuario usuario = sessao.atual().orElseThrow();
        usuarioLabel.setText(usuario.getNome() + " — " + usuario.getPerfil());

        configurarAcesso(usuario, Permissao.VENDAS, vendasTab, vendasNav);
        configurarAcesso(usuario, Permissao.CAIXA, caixaTab, caixaNav);
        configurarAcesso(usuario, Permissao.RELATORIOS, historicoTab, historicoNav);
        configurarAcesso(usuario, Permissao.RELATORIOS, relatoriosTab, relatoriosNav);
        configurarAcesso(usuario, Permissao.PRODUTOS, produtosTab, produtosNav);
        configurarAcesso(usuario, Permissao.ESTOQUE, estoqueTab, estoqueNav);
        configurarAcesso(usuario, Permissao.USUARIOS, usuariosTab, usuariosNav);
        configurarAcesso(usuario, Permissao.BACKUP, backupTab, backupNav);
        atualizarSecoesDaNavegacao();

        tabs.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, atual) -> {
                    sincronizarNavegacao(atual);
                    atualizarIndicadorCaixa();
                });
        selecionarPrimeiraAreaDisponivel();
        atualizarIndicadorCaixa();
    }

    @FXML private void showVendas() { selecionar(vendasTab, vendasNav); }
    @FXML private void showCaixa() { selecionar(caixaTab, caixaNav); }
    @FXML private void showHistorico() { selecionar(historicoTab, historicoNav); }
    @FXML private void showRelatorios() { selecionar(relatoriosTab, relatoriosNav); }
    @FXML private void showProdutos() { selecionar(produtosTab, produtosNav); }
    @FXML private void showEstoque() { selecionar(estoqueTab, estoqueNav); }
    @FXML private void showUsuarios() { selecionar(usuariosTab, usuariosNav); }
    @FXML private void showBackup() { selecionar(backupTab, backupNav); }

    /** Remove da interface a aba e o botao que o perfil nao pode acessar. */
    private void configurarAcesso(
            Usuario usuario, Permissao permissao, Tab tab, ToggleButton navigation) {
        boolean permitido = usuario.getPerfil().permite(permissao);
        if (!permitido) {
            tabs.getTabs().remove(tab);
        }
        navigation.setVisible(permitido);
        navigation.setManaged(permitido);
    }


    /** Oculta titulos de grupos que ficaram sem nenhuma opcao visivel. */
    private void atualizarSecoesDaNavegacao() {
        definirVisibilidade(consultasSection,
                historicoNav.isManaged() || relatoriosNav.isManaged());
        definirVisibilidade(gestaoSection,
                produtosNav.isManaged() || estoqueNav.isManaged() || usuariosNav.isManaged());
        definirVisibilidade(sistemaSection, backupNav.isManaged());
    }

    /** Mantem visible e managed sincronizados para nao deixar espacos vazios. */
    private void definirVisibilidade(Label label, boolean visivel) {
        label.setVisible(visivel);
        label.setManaged(visivel);
    }

    /** Seleciona uma aba pelo menu lateral e mantem o botao correspondente ativo. */
    private void selecionar(Tab tab, ToggleButton navigation) {
        if (!tabs.getTabs().contains(tab)) return;
        tabs.getSelectionModel().select(tab);
        navigation.setSelected(true);
    }

    /** Escolhe uma tela valida mesmo para perfis com poucas permissoes. */
    private void selecionarPrimeiraAreaDisponivel() {
        if (tabs.getTabs().isEmpty()) return;
        Tab selecionada = tabs.getSelectionModel().getSelectedItem();
        if (selecionada == null || !tabs.getTabs().contains(selecionada)) {
            tabs.getSelectionModel().selectFirst();
            selecionada = tabs.getSelectionModel().getSelectedItem();
        }
        sincronizarNavegacao(selecionada);
    }

    /** Atualiza o menu quando a aba e alterada por codigo ou teclado. */
    private void sincronizarNavegacao(Tab tab) {
        if (tab == vendasTab) vendasNav.setSelected(true);
        else if (tab == caixaTab) caixaNav.setSelected(true);
        else if (tab == historicoTab) historicoNav.setSelected(true);
        else if (tab == relatoriosTab) relatoriosNav.setSelected(true);
        else if (tab == produtosTab) produtosNav.setSelected(true);
        else if (tab == estoqueTab) estoqueNav.setSelected(true);
        else if (tab == usuariosTab) usuariosNav.setSelected(true);
        else if (tab == backupTab) backupNav.setSelected(true);
    }

    /** Consulta o caixa atual e atualiza o texto e a classe visual do indicador. */
    private void atualizarIndicadorCaixa() {
        try {
            boolean aberto = caixaService.buscarCaixaAtual().isPresent();
            caixaLabel.setText(aberto ? "● Caixa aberto" : "● Caixa fechado");
            caixaLabel.getStyleClass().removeAll("cash-open", "cash-closed");
            caixaLabel.getStyleClass().add(aberto ? "cash-open" : "cash-closed");
        } catch (RuntimeException exception) {
            caixaLabel.setText("Caixa indisponível");
            caixaLabel.getStyleClass().removeAll("cash-open", "cash-closed");
            caixaLabel.getStyleClass().add("cash-closed");
            ErrorHandler.mensagem(exception);
        }
    }
}
