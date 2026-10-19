package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;

/**
 * Fond "cérémonial" du tableau d'honneur (§document-school, "Tableau d'honneur.dc.html") : plein cadre
 * vert foncé, bandeau tricolore en haut (pas à gauche comme les autres documents — fidèle à la
 * référence), double cadre doré en retrait (plein + translucide). Dessiné sur la couche BACKGROUND
 * (sous le contenu), comme {@link BulletinPageBackground}.
 */
class HonorRollPageBackground extends PdfPageEventHelper {

    private static final Color DARK_GREEN = new Color(14, 77, 58);
    private static final Color GOLD = new Color(184, 137, 43);
    // Alpha ignoré par le flux de contenu PDF (pas d'ExtGState ici) — teinte pâle opaque équivalente.
    private static final Color GOLD_FAINT = new Color(103, 93, 68);
    private static final Color RED = new Color(206, 17, 38);
    private static final Color YELLOW = new Color(252, 209, 22);
    private static final Color GREEN = new Color(0, 148, 96);

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
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

    private static float mm(float v) { return v * 72f / 25.4f; }
}
