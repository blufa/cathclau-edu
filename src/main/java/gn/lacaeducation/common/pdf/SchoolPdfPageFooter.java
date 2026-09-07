package gn.lacaeducation.common.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantPublicInfo;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Pied de page officiel commun à tous les documents PDF : filet aux couleurs de l'école, mention
 * « Généré le … » et contact (téléphone/e-mail) à gauche, nom de l'établissement au centre et
 * pagination « Page X » à droite — reproduit le pied de page des documents pédagogiques papier
 * (référence : "Fiche de note 7eme.docx"). S'attache au {@link PdfWriter} via
 * {@code writer.setPageEvent(...)}.
 */
public class SchoolPdfPageFooter extends PdfPageEventHelper {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final String schoolName;
    private final String contact;
    private final Color brand;
    private final String generatedOn;

    /** @deprecated use {@link #SchoolPdfPageFooter(TenantPublicInfo, Color)} to also show the contact line. */
    @Deprecated
    public SchoolPdfPageFooter(String schoolName, Color brand) {
        this.schoolName = schoolName == null || schoolName.isBlank() ? "" : schoolName;
        this.contact = "";
        this.brand = brand == null ? SchoolPdfHeader.DEFAULT_BRAND : brand;
        this.generatedOn = "Généré le " + LocalDate.now().format(DATE);
    }

    public SchoolPdfPageFooter(TenantPublicInfo info, Color brand) {
        this.schoolName = info == null || info.schoolName() == null || info.schoolName().isBlank() ? "" : info.schoolName();
        String phone = info == null ? null : info.phone();
        String email = info == null ? null : info.email();
        StringBuilder c = new StringBuilder();
        if (phone != null && !phone.isBlank()) c.append(phone);
        if (email != null && !email.isBlank()) { if (c.length() > 0) c.append("  /  "); c.append(email); }
        this.contact = c.toString();
        this.brand = brand == null ? SchoolPdfHeader.DEFAULT_BRAND : brand;
        this.generatedOn = "Généré le " + LocalDate.now().format(DATE);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        Rectangle page = document.getPageSize();
        float left = document.leftMargin();
        float right = page.getWidth() - document.rightMargin();
        float y = document.bottomMargin() - 10f;

        // Filet séparateur aux couleurs de l'école.
        cb.saveState();
        cb.setColorStroke(brand);
        cb.setLineWidth(0.6f);
        cb.moveTo(left, y + 8f);
        cb.lineTo(right, y + 8f);
        cb.stroke();
        cb.restoreState();

        Font f = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, SchoolPdfHeader.GREY);
        String leftText = contact.isBlank() ? generatedOn : generatedOn + "   —   " + contact;
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(leftText, f), left, y, 0);
        if (!schoolName.isBlank()) {
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(schoolName, f), (left + right) / 2f, y, 0);
        }
        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                new Phrase("Page " + writer.getPageNumber(), f), right, y, 0);
    }
}
