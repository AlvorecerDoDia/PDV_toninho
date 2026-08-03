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

/** Envia o comprovante usando comandos ESC/POS. */
public final class ImpressoraWindows implements ImpressoraComprovante {

    private static final Charset CHARSET_IMPRESSORA =
            Charset.forName("IBM860");

    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    private static final byte LF = 0x0A;

    private final FormatadorComprovante formatador;

    public ImpressoraWindows(FormatadorComprovante formatador) {
        this.formatador = formatador;
    }

    @Override
    public void imprimir(Venda venda, boolean segundaVia) {
        PrintService impressora =
                PrintServiceLookup.lookupDefaultPrintService();

        if (impressora == null) {
            throw new ImpressaoException(
                    "Nenhuma impressora padrão foi encontrada. " +
                            "A venda continua salva."
            );
        }

        try {
            String texto = formatador
                    .formatar(venda, segundaVia)
                    .replace('\u00A0', ' ')
                    .replace('\u202F', ' ');

            ByteArrayOutputStream dados = new ByteArrayOutputStream();

            // Inicializa a impressora.
            dados.writeBytes(new byte[]{
                    ESC, 0x40
            });

            // Seleciona PC860 Portuguese.
            dados.writeBytes(new byte[]{
                    ESC, 0x74, 0x03
            });

            // Envia o texto do comprovante.
            dados.writeBytes(texto.getBytes(CHARSET_IMPRESSORA));

            // Garante que a ultima linha seja impressa.
            dados.write(LF);

            // GS V 66 20:
            // avanca o papel e realiza corte parcial.
            dados.writeBytes(new byte[]{
                    GS, 0x56, 0x42, 0x14
            });

            Doc documento = new SimpleDoc(
                    dados.toByteArray(),
                    DocFlavor.BYTE_ARRAY.AUTOSENSE,
                    null
            );

            impressora.createPrintJob().print(documento, null);

        } catch (PrintException exception) {
            throw new ImpressaoException(
                    "Não foi possível imprimir. " +
                            "A venda continua salva e pode ser reimpressa.",
                    exception
            );
        }
    }
}