package br.com.loja.pdv.repository.sqlite;

import br.com.loja.pdv.domain.enums.FormaPagamento;
import br.com.loja.pdv.domain.model.Pagamento;
import br.com.loja.pdv.exception.DatabaseException;
import br.com.loja.pdv.infrastructure.database.Database;
import br.com.loja.pdv.repository.PagamentoRepository;
import br.com.loja.pdv.util.MoneyUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class SQLitePagamentoRepository implements PagamentoRepository {
    private final Database database;

    public SQLitePagamentoRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<Pagamento> listarPorVenda(long vendaId) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT * FROM pagamento WHERE venda_id = ? ORDER BY id
                     """)) {
            statement.setLong(1, vendaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Pagamento> result = new ArrayList<>();
                while (resultSet.next()) {
                    Pagamento payment = new Pagamento();
                    payment.setId(resultSet.getLong("id"));
                    payment.setVendaId(resultSet.getLong("venda_id"));
                    payment.setForma(FormaPagamento.valueOf(resultSet.getString("forma")));
                    payment.setValor(MoneyUtils.fromCents(resultSet.getLong("valor_centavos")));
                    payment.setCriadoEm(LocalDateTime.parse(resultSet.getString("criado_em")));
                    result.add(payment);
                }
                return result;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Não foi possível consultar os pagamentos.", exception);
        }
    }
}
