package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.Permissao;
import br.com.loja.pdv.infrastructure.backup.GerenciadorBackup;

import java.nio.file.Path;
import java.util.List;

public final class BackupService {
    private static final int RETENCAO_PADRAO = 20;
    private final GerenciadorBackup gerenciador;
    private final SessaoUsuario sessao;

    public BackupService(GerenciadorBackup gerenciador, SessaoUsuario sessao) {
        this.gerenciador = gerenciador;
        this.sessao = sessao;
    }

    public Path criarManual() {
        sessao.exigir(Permissao.BACKUP);
        Path backup = gerenciador.criar("manual");
        gerenciador.aplicarRetencao(RETENCAO_PADRAO);
        return backup;
    }

    public Path criarAutomatico() {
        Path backup = gerenciador.criar("automatico");
        gerenciador.aplicarRetencao(RETENCAO_PADRAO);
        return backup;
    }

    public Path restaurar(Path arquivo) {
        sessao.exigir(Permissao.BACKUP);
        return gerenciador.restaurar(arquivo);
    }

    public List<Path> listar() {
        sessao.exigir(Permissao.BACKUP);
        return gerenciador.listar();
    }
}
