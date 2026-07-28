package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.enums.TipoRelatorio;
import br.com.loja.pdv.domain.model.FiltroRelatorio;
import br.com.loja.pdv.domain.model.LinhaRelatorio;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.repository.RelatorioRepository;
import br.com.loja.pdv.util.MoneyUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Executa as consultas agregadas e converte centavos para BigDecimal. */
public final class SQLiteRelatorioRepository implements RelatorioRepository {
    private final Database database;

    public SQLiteRelatorioRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<LinhaRelatorio> gerar(TipoRelatorio tipo, FiltroRelatorio filtro) {
        return switch (tipo) {
            case VENDAS_POR_DIA -> vendasPorDia(filtro);
            case VENDAS_POR_PERIODO -> vendasPorPeriodo(filtro);
            case VENDAS_POR_OPERADOR -> vendasPorOperador(filtro);
            case TOTAL_POR_FORMA_PAGAMENTO -> pagamentos(filtro);
            case PRODUTOS_MAIS_VENDIDOS -> produtosMaisVendidos(filtro);
            case ESTOQUE_BAIXO -> estoqueBaixo(filtro);
            case MOVIMENTACOES_ESTOQUE -> movimentacoesEstoque(filtro);
            case DESCONTOS -> descontos(filtro);
            case CANCELAMENTOS -> cancelamentos(filtro);
            case FECHAMENTO_CAIXA -> fechamentos(filtro);
            case LUCRO_BRUTO_ESTIMADO -> lucro(filtro);
        };
    }

    private List<LinhaRelatorio> vendasPorDia(FiltroRelatorio filter) {
        return query("""
                SELECT date(v.criado_em) categoria, '' detalhe, COUNT(*) quantidade,
                       SUM(v.total_centavos) valor, NULL valor2, MIN(v.criado_em) data
                FROM venda v
                WHERE v.status = 'FINALIZADA' AND v.criado_em BETWEEN ? AND ?
                  AND (? IS NULL OR v.operador_id = ?)
                GROUP BY date(v.criado_em) ORDER BY date(v.criado_em)
                """, filter, false, false);
    }

    private List<LinhaRelatorio> vendasPorPeriodo(FiltroRelatorio filter) {
        return query("""
                SELECT v.numero categoria, u.nome detalhe, 1 quantidade,
                       v.total_centavos valor, v.desconto_centavos valor2, v.criado_em data
                FROM venda v JOIN usuario u ON u.id = v.operador_id
                WHERE v.status = 'FINALIZADA' AND v.criado_em BETWEEN ? AND ?
                  AND (? IS NULL OR v.operador_id = ?)
                ORDER BY v.criado_em
                """, filter, false, false);
    }

    private List<LinhaRelatorio> vendasPorOperador(FiltroRelatorio filter) {
        return query("""
                SELECT u.nome categoria, u.login detalhe, COUNT(*) quantidade,
                       SUM(v.total_centavos) valor, SUM(v.desconto_centavos) valor2,
                       MIN(v.criado_em) data
                FROM venda v JOIN usuario u ON u.id = v.operador_id
                WHERE v.status = 'FINALIZADA' AND v.criado_em BETWEEN ? AND ?
                  AND (? IS NULL OR v.operador_id = ?)
                GROUP BY u.id, u.nome, u.login ORDER BY valor DESC
                """, filter, false, false);
    }

    private List<LinhaRelatorio> pagamentos(FiltroRelatorio filter) {
        String sql = """
                SELECT p.forma categoria, '' detalhe, COUNT(*) quantidade,
                       SUM(p.valor_centavos) valor, NULL valor2, MIN(v.criado_em) data
                FROM pagamento p JOIN venda v ON v.id = p.venda_id
                WHERE v.status = 'FINALIZADA' AND v.criado_em BETWEEN ? AND ?
                  AND (? IS NULL OR v.operador_id = ?)
                  AND (? IS NULL OR p.forma = ?)
                GROUP BY p.forma ORDER BY p.forma
                """;
        return query(sql, filter, true, false);
    }

    private List<LinhaRelatorio> produtosMaisVendidos(FiltroRelatorio filter) {
        String sql = """
                SELECT i.produto_nome categoria, CAST(i.produto_id AS TEXT) detalhe,
                       SUM(i.quantidade) quantidade, SUM(i.subtotal_centavos) valor,
                       SUM(i.custo_unitario_centavos * i.quantidade) valor2,
                       MIN(v.criado_em) data
                FROM item_venda i JOIN venda v ON v.id = i.venda_id
                WHERE v.status = 'FINALIZADA' AND v.criado_em BETWEEN ? AND ?
                  AND (? IS NULL OR v.operador_id = ?)
                  AND (? IS NULL OR i.produto_id = ?)
                GROUP BY i.produto_id, i.produto_nome ORDER BY quantidade DESC
                """;
        return query(sql, filter, false, true);
    }

    private List<LinhaRelatorio> estoqueBaixo(FiltroRelatorio filter) {
        String sql = """
                SELECT p.nome categoria,
                       'Mínimo: ' || p.estoque_minimo detalhe,
                       p.quantidade_estoque quantidade,
                       p.preco_venda_centavos valor, NULL valor2, p.atualizado_em data
                FROM produto p
                WHERE p.ativo = 1 AND p.quantidade_estoque <= p.estoque_minimo
                  AND (? IS NULL OR p.id = ?)
                ORDER BY p.quantidade_estoque, p.nome
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindNullable(statement, 1, 2, filter.produtoId());
            return read(statement);
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    private List<LinhaRelatorio> movimentacoesEstoque(FiltroRelatorio filter) {
        String sql = """
                SELECT p.nome categoria,
                       m.tipo || COALESCE(' — ' || m.motivo, '') detalhe,
                       m.quantidade quantidade, NULL valor, NULL valor2, m.criado_em data
                FROM movimentacao_estoque m JOIN produto p ON p.id = m.produto_id
                WHERE m.criado_em BETWEEN ? AND ?
                  AND (? IS NULL OR m.usuario_id = ?)
                  AND (? IS NULL OR m.produto_id = ?)
                ORDER BY m.criado_em
                """;
        return query(sql, filter, false, true);
    }

    private List<LinhaRelatorio> descontos(FiltroRelatorio filter) {
        return query("""
                SELECT v.numero categoria, u.nome detalhe, 1 quantidade,
                       v.desconto_centavos valor, v.total_centavos valor2, v.criado_em data
                FROM venda v JOIN usuario u ON u.id = v.operador_id
                WHERE v.status = 'FINALIZADA' AND v.desconto_centavos > 0
                  AND v.criado_em BETWEEN ? AND ?
                  AND (? IS NULL OR v.operador_id = ?)
                ORDER BY v.criado_em
                """, filter, false, false);
    }

    private List<LinhaRelatorio> cancelamentos(FiltroRelatorio filter) {
        return query("""
                SELECT v.numero categoria, v.motivo_cancelamento detalhe, 1 quantidade,
                       v.total_centavos valor, v.desconto_centavos valor2, v.cancelado_em data
                FROM venda v
                WHERE v.status = 'CANCELADA' AND v.cancelado_em BETWEEN ? AND ?
                  AND (? IS NULL OR v.operador_id = ?)
                ORDER BY v.cancelado_em
                """, filter, false, false);
    }

    private List<LinhaRelatorio> fechamentos(FiltroRelatorio filter) {
        return query("""
                SELECT u.nome categoria,
                       'Caixa #' || c.id || ' | Diferença (centavos): '
                           || c.diferenca_centavos detalhe,
                       1 quantidade, c.valor_contado_centavos valor,
                       c.valor_esperado_centavos valor2, c.fechado_em data
                FROM caixa c JOIN usuario u ON u.id = c.usuario_id
                WHERE c.status = 'FECHADO' AND c.fechado_em BETWEEN ? AND ?
                  AND (? IS NULL OR c.usuario_id = ?)
                ORDER BY c.fechado_em
                """, filter, false, false);
    }

    private List<LinhaRelatorio> lucro(FiltroRelatorio filter) {
        return query("""
                WITH por_venda AS (
                    SELECT v.id, v.criado_em,
                           SUM((i.preco_unitario_centavos - i.custo_unitario_centavos)
                               * i.quantidade) - v.desconto_centavos lucro
                    FROM venda v JOIN item_venda i ON i.venda_id = v.id
                    WHERE v.status = 'FINALIZADA' AND v.criado_em BETWEEN ? AND ?
                      AND (? IS NULL OR v.operador_id = ?)
                    GROUP BY v.id, v.criado_em, v.desconto_centavos
                )
                SELECT 'Lucro bruto estimado' categoria, '' detalhe,
                       COUNT(*) quantidade, COALESCE(SUM(lucro), 0) valor,
                       NULL valor2, MIN(criado_em) data FROM por_venda
                """, filter, false, false);
    }

    private List<LinhaRelatorio> query(
            String sql, FiltroRelatorio filter, boolean bindPayment, boolean bindProduct) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, filter.inicio().atStartOfDay().toString());
            statement.setString(2, filter.fim().plusDays(1).atStartOfDay().minusNanos(1).toString());
            bindNullable(statement, 3, 4, filter.operadorId());
            int next = 5;
            if (bindPayment) {
                String value = filter.formaPagamento() == null
                        ? null : filter.formaPagamento().name();
                bindNullableText(statement, next, next + 1, value);
                next += 2;
            }
            if (bindProduct) bindNullable(statement, next, next + 1, filter.produtoId());
            return read(statement);
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    private List<LinhaRelatorio> read(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<LinhaRelatorio> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new LinhaRelatorio(
                        resultSet.getString("categoria"),
                        resultSet.getString("detalhe"),
                        nullableLong(resultSet, "quantidade"),
                        nullableMoney(resultSet, "valor"),
                        nullableMoney(resultSet, "valor2"),
                        nullableDateTime(resultSet, "data")));
            }
            return result;
        }
    }

    private void bindNullable(
            PreparedStatement statement, int nullIndex, int valueIndex, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(nullIndex, Types.BIGINT);
            statement.setNull(valueIndex, Types.BIGINT);
        } else {
            statement.setLong(nullIndex, value);
            statement.setLong(valueIndex, value);
        }
    }

    private void bindNullableText(
            PreparedStatement statement, int nullIndex, int valueIndex, String value)
            throws SQLException {
        if (value == null) {
            statement.setNull(nullIndex, Types.VARCHAR);
            statement.setNull(valueIndex, Types.VARCHAR);
        } else {
            statement.setString(nullIndex, value);
            statement.setString(valueIndex, value);
        }
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private BigDecimal nullableMoney(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : MoneyUtils.fromCents(value);
    }

    private LocalDateTime nullableDateTime(ResultSet resultSet, String column)
            throws SQLException {
        String value = resultSet.getString(column);
        if (value == null || value.length() == 10) return null;
        return LocalDateTime.parse(value);
    }

    private DatabaseException failure(SQLException exception) {
        return new DatabaseException("Não foi possível gerar o relatório.", exception);
    }
}
