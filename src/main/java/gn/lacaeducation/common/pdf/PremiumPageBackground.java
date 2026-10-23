package gn.lacaeducation.common.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;

/** Fond crème + liseré tricolore (drapeau guinéen) sur le bord gauche de chaque page — reproduit
 * fidèlement le design de référence (§retour fondateur, /features/document-school/*.dc.html). Dessiné
 * en tout premier (sous le contenu) via le canevas "under" du {@link PdfWriter}. Public (partagé entre
 * modules) : utilisé par le bulletin/la fiche de notes/le centralisateur (module {@code grading}) ET
 * la fiche de paie (module {@code payroll}), toutes des déclinaisons du même design "premium" de
 * référence — jamais un bandeau de couleur dynamique (contrairement à {@code RoundedCellBorder}, le
 * fond lui-même reste fidèle à la référence pour tous les documents qui l'utilisent).
 */
public final class PremiumPageBackground extends PdfPageEventHelper {

    public static final Color CREAM = new Color(255, 253, 248);
    private static final Color RED = new Color(206, 17, 38);
    private static final Color YELLOW = new Color(252, 209, 22);
    private static final Color GREEN = new Color(0, 148, 96);
    private static final float STRIPE_WIDTH = 8.5f; // 3mm

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContentUnder();
        Rectangle page = document.getPageSize();

        cb.saveState();
        cb.setColorFill(CREAM);
        cb.rectangle(0, 0, page.getWidth(), page.getHeight());
        cb.fill();
        cb.restoreState();

        float third = page.getHeight() / 3f;
        cb.saveState();
        cb.setColorFill(RED);
        cb.rectangle(0, page.getHeight() - third, STRIPE_WIDTH, third);
        cb.fill();
        cb.setColorFill(YELLOW);
        cb.rectangle(0, third, STRIPE_WIDTH, third);
        cb.fill();
        cb.setColorFill(GREEN);
        cb.rectangle(0, 0, STRIPE_WIDTH, third);
        cb.fill();
        cb.restoreState();
    }
}
