package br.com.loja.pdv.infrastructure.printing;

import br.com.loja.pdv.domain.model.Venda;
import br.com.loja.pdv.exception.ImpressaoException;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/** Envia o comprovante usando a tabela portuguesa e comandos ESC/POS. */
public final class ImpressoraWindows implements ImpressoraComprovante {
    private static final Charset CHARSET_IMPRESSORA = Charset.forName("IBM860");
    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    private static final byte LF = 0x0A;

    private final FormatadorComprovante formatador;

    public ImpressoraWindows(FormatadorComprovante formatador) {
        this.formatador = formatador;
    }

    /** Imprime, avanca o papel e solicita corte parcial ao autocutter. */
    @Override
    public void imprimir(Venda venda, boolean segundaVia) {
        PrintService impressora = PrintServiceLookup.lookupDefaultPrintService();
        if (impressora == null) {
            throw new ImpressaoException(
                    "Nenhuma impressora padrão foi encontrada. A venda continua salva.");
        }

        String texto = formatador.formatar(venda, segundaVia)
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ');

        ByteArrayOutputStream dados = new ByteArrayOutputStream();
        // ESC @ reinicia o estado do equipamento.
        dados.writeBytes(new byte[]{ESC, 0x40});
        // ESC t 3 seleciona PC860 Portuguese nas impressoras Epson compativeis.
        dados.writeBytes(new byte[]{ESC, 0x74, 0x03});
        dados.writeBytes(texto.getBytes(CHARSET_IMPRESSORA));
        dados.write(LF);
        // GS V 66 20 avanca e realiza corte parcial.
        dados.writeBytes(new byte[]{GS, 0x56, 0x42, 0x14});

        Doc document = new SimpleDoc(
                dados.toByteArray(), DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        try {
            impressora.createPrintJob().print(document, null);
        } catch (PrintException exception) {
            throw new ImpressaoException(
                    "Não foi possível imprimir. A venda continua salva e pode ser reimpressa.",
                    exception);
        }
    }
}
