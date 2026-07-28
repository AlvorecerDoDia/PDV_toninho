package br.com.loja.pdv.infrastructure.reporting;

import br.com.loja.pdv.domain.enums.TipoRelatorio;
import br.com.loja.pdv.domain.model.LinhaRelatorio;
import br.com.loja.pdv.exception.DatabaseException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ExportadorCsv {
    private static final NumberFormat DECIMAL =
            NumberFormat.getNumberInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public void exportar(
            Path destination, TipoRelatorio type, List<LinhaRelatorio> rows) {
        if (destination == null) throw new IllegalArgumentException("Escolha o arquivo CSV.");
        try {
            Path parent = destination.toAbsolutePath().normalize().getParent();
            if (parent != null) Files.createDirectories(parent);
            try (var writer = Files.newBufferedWriter(
                    destination, StandardCharsets.UTF_8)) {
                writer.write('\ufeff');
                writer.write("Relatório;Categoria;Detalhe;Quantidade;Valor;Valor secundário;Data");
                writer.newLine();
                for (LinhaRelatorio row : rows) {
                    writer.write(csv(type.name()));
                    writer.write(';');
                    writer.write(csv(row.categoria()));
                    writer.write(';');
                    writer.write(csv(row.detalhe()));
                    writer.write(';');
                    writer.write(row.quantidade() == null ? "" : row.quantidade().toString());
                    writer.write(';');
                    writer.write(money(row.valor()));
                    writer.write(';');
                    writer.write(money(row.valorSecundario()));
                    writer.write(';');
                    writer.write(row.data() == null ? "" : row.data().format(DATE_TIME));
                    writer.newLine();
                }
            }
        } catch (IOException exception) {
            throw new DatabaseException("Não foi possível exportar o relatório.", exception);
        }
    }

    private String money(BigDecimal value) {
        return value == null ? "" : DECIMAL.format(value);
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }
}
