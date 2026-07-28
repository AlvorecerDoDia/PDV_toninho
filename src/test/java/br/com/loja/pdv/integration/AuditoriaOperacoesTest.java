package br.com.loja.pdv.integration;

import br.com.loja.pdv.domain.enums.PerfilUsuario;
import br.com.loja.pdv.domain.enums.TipoMovimentacaoEstoque;
import br.com.loja.pdv.domain.model.Produto;
import br.com.loja.pdv.domain.model.Usuario;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.infrastructure.database.DatabaseInitializer;
import br.com.loja.pdv.infrastructure.security.PasswordHasher;
import br.com.loja.pdv.repository.sqlite.*;
import br.com.loja.pdv.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaOperacoesTest {
    @TempDir Path tempDirectory;
    private Database database;
    private SessaoUsuario sessao;
    private AuditoriaService auditoria;
    private SQLiteUsuarioRepository usuarios;

    @BeforeEach
    void setUp() {
        database = new Database(tempDirectory.resolve("auditoria-operacoes.db"));
        new DatabaseInitializer(database).initialize();
        usuarios = new SQLiteUsuarioRepository(database);
        Usuario administrador = new UsuarioService(
                usuarios, new PasswordHasher()).criar(
                "Administrador", "admin", "SenhaForte1".toCharArray(),
                PerfilUsuario.ADMINISTRADOR, false);
        sessao = new SessaoUsuario();
        sessao.iniciar(administrador);
        auditoria = new AuditoriaService(
                new SQLiteAuditoriaRepository(database), sessao);
    }

    @Test
    void deveAuditarPrecoEstoqueCaixaEUsuarioSemSenhaOuHash() {
        SQLiteProdutoRepository produtos = new SQLiteProdutoRepository(database);
        ProdutoService produtoService = new ProdutoService(produtos, auditoria);
        Produto produto = new Produto();
        produto.setNome("Café");
        produto.setPrecoCusto(new BigDecimal("10.00"));
        produto.setPrecoVenda(new BigDecimal("15.00"));
        produto.setEstoqueMinimo(2);
        produto = produtoService.cadastrar(produto);

        Produto alterado = produtoService.buscarPorId(produto.getId());
        alterado.setPrecoVenda(new BigDecimal("16.00"));
        produtoService.atualizar(alterado);

        EstoqueService estoque = new EstoqueService(
                new SQLiteEstoqueRepository(database), produtos, sessao, auditoria);
        estoque.registrar(produto.getId(), TipoMovimentacaoEstoque.ENTRADA, 10, null);
        estoque.registrar(
                produto.getId(), TipoMovimentacaoEstoque.AJUSTE_NEGATIVO, 1,
                "Contagem física");

        CaixaService caixa = new CaixaService(
                new SQLiteCaixaRepository(database), sessao, auditoria);
        caixa.abrir(BigDecimal.ZERO);
        caixa.suprir(new BigDecimal("20.00"), "Fundo adicional");
        caixa.sangrar(new BigDecimal("5.00"), "Retirada preventiva");

        UsuarioService usuarioService = new UsuarioService(
                usuarios, new PasswordHasher(), auditoria);
        Usuario operador = usuarioService.criar(
                "Operador", "operador", "SenhaForte2".toCharArray(),
                PerfilUsuario.OPERADOR, false);
        usuarioService.atualizar(
                operador.getId(), "Operador Atualizado", "operador",
                PerfilUsuario.OPERADOR, true);
        usuarioService.trocarSenha(
                operador.getId(), "OutraSenha3".toCharArray());

        var registros = auditoria.listarRecentes(100);
        Set<String> acoes = registros.stream()
                .map(registro -> registro.getAcao())
                .collect(Collectors.toSet());
        assertTrue(acoes.contains("ALTERACAO_PRECO"));
        assertTrue(acoes.contains("AJUSTE_ESTOQUE"));
        assertTrue(acoes.contains("SUPRIMENTO"));
        assertTrue(acoes.contains("SANGRIA"));
        assertTrue(acoes.contains("ALTERACAO_USUARIO"));
        assertTrue(acoes.contains("TROCA_SENHA"));
        assertTrue(registros.stream().noneMatch(registro ->
                text(registro).contains("$2") || text(registro).contains("hash=")));
    }

    private String text(br.com.loja.pdv.domain.model.RegistroAuditoria registro) {
        return String.valueOf(registro.getValoresAnteriores())
                + String.valueOf(registro.getValoresNovos());
    }
}
