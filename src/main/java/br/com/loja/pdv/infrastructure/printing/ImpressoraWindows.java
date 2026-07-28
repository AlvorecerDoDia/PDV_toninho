package br.com.loja.pdv.infrastructure.printing;

import br.com.loja.pdv.domain.model.Venda;
import br.com.loja.pdv.exception.ImpressaoException;

import javax.print.*;
import java.nio.charset.StandardCharsets;

public final class ImpressoraWindows implements ImpressoraComprovante {
    private final FormatadorComprovante formatador;

    public ImpressoraWindows(FormatadorComprovante formatador) {
        this.formatador = formatador;
    }

    @Override
    public void imprimir(Venda venda, boolean segundaVia) {
        PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
        if (printer == null) {
            throw new ImpressaoException(
                    "Nenhuma impressora padrão foi encontrada. A venda continua salva.");
        }
        byte[] content = formatador.formatar(venda, segundaVia)
                .getBytes(StandardCharsets.UTF_8);
        Doc document = new SimpleDoc(
                content, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        try {
            printer.createPrintJob().print(document, null);
        } catch (PrintException exception) {
            throw new ImpressaoException(
                    "Não foi possível imprimir. A venda continua salva e pode ser reimpressa.",
                    exception);
        }
    }
}
