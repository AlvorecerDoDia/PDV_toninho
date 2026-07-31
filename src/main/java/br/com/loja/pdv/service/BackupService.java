package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.enums.Permissao;
import br.com.loja.pdv.infrastructure.backup.GerenciadorBackup;

import java.nio.file.Path;
import java.util.List;

/**
 * Coordena criacao, retencao e restauracao de backups conforme as permissoes do usuario.
 */
public final class BackupService {
    private static final int RETENCAO_PADRAO = 20;
    private final GerenciadorBackup gerenciador;
    private final SessaoUsuario sessao;
    private final AuditoriaService auditoria;

    public BackupService(GerenciadorBackup gerenciador, SessaoUsuario sessao) {
        this(gerenciador, sessao, null);
    }

    public BackupService(
            GerenciadorBackup gerenciador, SessaoUsuario sessao,
            AuditoriaService auditoria) {
        this.gerenciador = gerenciador;
        this.sessao = sessao;
        this.auditoria = auditoria;
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
        Path seguranca = gerenciador.restaurar(arquivo);
        if (auditoria != null) {
            auditoria.registrar(
                    "RESTAURACAO_BACKUP", "BANCO", null,
                    "copia_seguranca=" + seguranca.getFileName(),
                    "arquivo=" + arquivo.toAbsolutePath().normalize().getFileName());
        }
        return seguranca;
    }

    public List<Path> listar() {
        sessao.exigir(Permissao.BACKUP);
        return gerenciador.listar();
    }
}
