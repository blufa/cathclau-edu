package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;

/**
 * Fond du document "Tableau d'honneur + Satisfecit" (§document-school, "Tableau d'honneur.dc.html" et
 * "satifaicit-tableau-honneur.pdf") : la page 1 (affiche cérémoniale) a un fond plein vert foncé avec
 * bandeau tricolore en haut ; les pages suivantes (un certificat "Satisfecit" par lauréat) ont un fond
 * crème avec un cadre décoratif doré/vert à coins arrondis. Dessiné sur la couche BACKGROUND (sous le
 * contenu), comme {@link PremiumPageBackground}.
 */
class HonorRollPageBackground extends PdfPageEventHelper {

    private static final Color DARK_GREEN = new Color(14, 77, 58);
    private static final Color GOLD = new Color(184, 137, 43);
    // Alpha ignoré par le flux de contenu PDF (pas d'ExtGState ici) — teinte pâle opaque équivalente.
    private static final Color GOLD_FAINT = new Color(103, 93, 68);
    private static final Color RED = new Color(206, 17, 38);
    private static final Color YELLOW = new Color(252, 209, 22);
    private static final Color GREEN = new Color(0, 148, 96);
    private static final Color CREAM = new Color(255, 253, 248);

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        if (writer.getPageNumber() == 1) {
            ceremonialBackground(writer, document);
        } else {
            satisfecitBackground(writer, document);
        }
    }

    private void ceremonialBackground(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContentUnder();
        float w = document.getPageSize().getWidth();
        float h = document.getPageSize().getHeight();

        cb.saveState();
        cb.setColorFill(DARK_GREEN);
        cb.rectangle(0, 0, w, h);
        cb.fill();

        float stripeH = mm(2);
        float third = w / 3f;
        cb.setColorFill(RED);
        cb.rectangle(0, h - stripeH, third, stripeH);
        cb.fill();
        cb.setColorFill(YELLOW);
        cb.rectangle(third, h - stripeH, third, stripeH);
        cb.fill();
        cb.setColorFill(GREEN);
        cb.rectangle(2 * third, h - stripeH, third, stripeH);
        cb.fill();

        float inset1 = mm(4);
        cb.setColorStroke(GOLD);
        cb.setLineWidth(mm(0.4f));
        cb.roundRectangle(inset1, inset1, w - 2 * inset1, h - 2 * inset1, mm(3));
        cb.stroke();

        float inset2 = mm(5.5f);
        cb.setColorStroke(GOLD_FAINT);
        cb.setLineWidth(mm(0.2f));
        cb.roundRectangle(inset2, inset2, w - 2 * inset2, h - 2 * inset2, mm(2.4f));
        cb.stroke();

        cb.restoreState();
    }

    /** Certificat "Satisfecit" : fond crème, cadre vert à coins arrondis, liseré doré en retrait avec
     * quatre fioritures en arc de cercle dans les coins — reproduit la maquette de référence. */
    private void satisfecitBackground(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContentUnder();
        float w = document.getPageSize().getWidth();
        float h = document.getPageSize().getHeight();

        cb.saveState();
        cb.setColorFill(CREAM);
        cb.rectangle(0, 0, w, h);
        cb.fill();

        float inset1 = mm(6);
        cb.setColorStroke(DARK_GREEN);
        cb.setLineWidth(mm(0.5f));
        cb.roundRectangle(inset1, inset1, w - 2 * inset1, h - 2 * inset1, mm(4));
        cb.stroke();

        float inset2 = mm(8);
        cb.setColorStroke(GOLD);
        cb.setLineWidth(mm(0.25f));
        cb.roundRectangle(inset2, inset2, w - 2 * inset2, h - 2 * inset2, mm(3));
        cb.stroke();

        float r = mm(7);
        float cornerInset = mm(9);
        cb.setColorStroke(GOLD);
        cb.setLineWidth(mm(0.3f));
        cb.arc(cornerInset - r, h - cornerInset - r, cornerInset + r, h - cornerInset + r, 180, 90);
        cb.stroke();
        cb.arc(w - cornerInset - r, h - cornerInset - r, w - cornerInset + r, h - cornerInset + r, 90, 90);
        cb.stroke();
        cb.arc(cornerInset - r, cornerInset - r, cornerInset + r, cornerInset + r, 180, -90);
        cb.stroke();
        cb.arc(w - cornerInset - r, cornerInset - r, w - cornerInset + r, cornerInset + r, 0, -90);
        cb.stroke();

        cb.restoreState();
    }

    private static float mm(float v) { return v * 72f / 25.4f; }
}
