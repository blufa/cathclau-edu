package gn.lacaeducation.common.pdf;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;

/** Dessine un fond et/ou une bordure à coins arrondis pour une {@link PdfPCell} — OpenPDF ne supporte
 * pas nativement les coins arrondis sur une cellule (seulement des rectangles droits), d'où cet
 * événement personnalisé. Partagé par les documents "design premium" (§retour fondateur, refonte des
 * documents de /features/document-school) : bulletin, fiche de notes, tableau d'honneur, fiche de
 * paie, etc. — n'utilise PAS {@code cell.setBackgroundColor}/{@code setBorder} (carrés, dessinés
 * par-dessus). Public (partagé entre modules) : déplacé de {@code grading.adapters.out.pdf} vers ce
 * paquet commun quand la fiche de paie (module {@code payroll}) en a eu besoin à son tour.
 */
public final class RoundedCellBorder implements PdfPCellEvent {

    private final float radius;
    private final Color fillColor;
    private final Color borderColor;
    private final float borderWidth;

    private RoundedCellBorder(float radius, Color fillColor, Color borderColor, float borderWidth) {
        this.radius = radius;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;
    }

    public static RoundedCellBorder fillAndBorder(float radius, Color fill, Color border, float borderWidth) {
        return new RoundedCellBorder(radius, fill, border, borderWidth);
    }

    public static RoundedCellBorder fillOnly(float radius, Color fill) {
        return new RoundedCellBorder(radius, fill, null, 0);
    }

    public static RoundedCellBorder borderOnly(float radius, Color border, float borderWidth) {
        return new RoundedCellBorder(radius, null, border, borderWidth);
    }

    @Override
    public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
        if (fillColor != null) {
            PdfContentByte bg = canvases[PdfPTable.BACKGROUNDCANVAS];
            bg.saveState();
            bg.setColorFill(fillColor);
            bg.roundRectangle(position.getLeft(), position.getBottom(), position.getWidth(), position.getHeight(), radius);
            bg.fill();
            bg.restoreState();
        }
        if (borderColor != null && borderWidth > 0) {
            PdfContentByte line = canvases[PdfPTable.LINECANVAS];
            line.saveState();
            line.setColorStroke(borderColor);
            line.setLineWidth(borderWidth);
            float inset = borderWidth / 2f;
            line.roundRectangle(position.getLeft() + inset, position.getBottom() + inset,
                    position.getWidth() - borderWidth, position.getHeight() - borderWidth, radius);
            line.stroke();
            line.restoreState();
        }
    }
}
