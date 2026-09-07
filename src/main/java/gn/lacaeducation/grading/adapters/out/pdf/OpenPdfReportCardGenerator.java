package gn.lacaeducation.grading.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.grading.application.port.out.ReportCardPdfGenerator;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

/** Renders a professional, branded report card (bulletin) to PDF using OpenPDF. */
@Component
@RequiredArgsConstructor
class OpenPdfReportCardGenerator implements ReportCardPdfGenerator {

    private static final Color GREY = SchoolPdfHeader.GREY;
    private static final Color DARK = SchoolPdfHeader.DARK;
    private static final Color ZEBRA = new Color(247, 248, 250);

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(ReportCard rc, List<ReportCardLine> lines, ReportCardContext ctx) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 40, 54);
        try {
            TenantPublicInfo info = ctx.tenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(ctx.tenantId()).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());
            String schoolName = info == null ? null : info.schoolName();

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            SchoolPdfHeader.render(doc, tenantInfoProvider, ctx.tenantId(), "BULLETIN SCOLAIRE");

            // ── Titre ──────────────────────────────────────────────────────────
            Paragraph title = new Paragraph("BULLETIN SCOLAIRE", SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 16, brand));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(6f);
            doc.add(title);

            String sub = join(" · ", ctx.periodLabel(), ctx.schoolYear());
            if (!sub.isBlank()) {
                Paragraph subtitle = new Paragraph(sub, SchoolPdfHeader.font(FontFactory.HELVETICA, 10, GREY));
                subtitle.setAlignment(Element.ALIGN_CENTER);
                subtitle.setSpacingAfter(10f);
                doc.add(subtitle);
            }

            // ── Identité de l'élève ────────────────────────────────────────────
            PdfPTable ident = new PdfPTable(new float[]{1f, 1f, 1f});
            ident.setWidthPercentage(100);
            ident.setSpacingAfter(12f);
            identCell(ident, "Élève", safe(ctx.studentName(), "—"), brand);
            identCell(ident, "Matricule", safe(ctx.studentNumber(), "—"), brand);
            identCell(ident, "Classe", safe(ctx.className(), "—"), brand);
            doc.add(ident);

            // ── Tableau des matières ───────────────────────────────────────────
            // Colonnes Cours/Composition affichées seulement si la formule active les renseigne
            // (étapes étiquetées §StepRole) — sinon tableau classique, aucune régression visuelle.
            boolean showCoursCompo = lines.stream().anyMatch(l -> l.getCoursAverage() != null || l.getCompoAverage() != null);
            int columns = showCoursCompo ? 7 : 5;
            PdfPTable table = new PdfPTable(showCoursCompo
                    ? new float[]{2.6f, 1f, 1f, 1f, 0.9f, 1.3f, 0.9f}
                    : new float[]{3.2f, 1.2f, 1f, 1.5f, 1f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(12f);
            String[] headers = showCoursCompo
                    ? new String[]{"Matière", "Cours", "Compo.", "Moyenne", "Coef.", "Moy. pondérée", "Rang"}
                    : new String[]{"Matière", "Moyenne", "Coef.", "Moy. pondérée", "Rang"};
            for (String h : headers) {
                headerCell(table, h, brand);
            }
            boolean zebra = false;
            for (ReportCardLine line : lines) {
                Color bg = zebra ? ZEBRA : Color.WHITE;
                zebra = !zebra;
                String subject = ctx.subjectName(line.getSubjectId());
                if (line.getTeacherName() != null && !line.getTeacherName().isBlank()) {
                    subject += "\n" + line.getTeacherName();
                }
                bodyCell(table, subject, Element.ALIGN_LEFT, bg, line.getTeacherName() != null);
                if (showCoursCompo) {
                    bodyCell(table, dec(line.getCoursAverage()), Element.ALIGN_CENTER, bg, false);
                    bodyCell(table, dec(line.getCompoAverage()), Element.ALIGN_CENTER, bg, false);
                }
                bodyCell(table, dec(line.getSubjectAverage()), Element.ALIGN_CENTER, bg, false);
                bodyCell(table, dec(line.getCoefficient()), Element.ALIGN_CENTER, bg, false);
                bodyCell(table, dec(line.getWeightedAverage()), Element.ALIGN_CENTER, bg, false);
                bodyCell(table, str(line.getSubjectRank()), Element.ALIGN_CENTER, bg, false);
            }
            if (lines.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Aucune note pour cette période.",
                        SchoolPdfHeader.font(FontFactory.HELVETICA_OBLIQUE, 9.5f, GREY)));
                empty.setColspan(columns);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                empty.setPadding(10f);
                table.addCell(empty);
            }
            doc.add(table);

            // ── Synthèse : moyenne générale + statistiques de classe ───────────
            PdfPTable synth = new PdfPTable(new float[]{1.3f, 2.7f});
            synth.setWidthPercentage(100);
            synth.setSpacingAfter(10f);

            PdfPCell avg = new PdfPCell();
            avg.setBackgroundColor(brand);
            avg.setBorder(0);
            avg.setPadding(12f);
            avg.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph avgLabel = new Paragraph("MOYENNE GÉNÉRALE", SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE));
            avgLabel.setAlignment(Element.ALIGN_CENTER);
            avg.addElement(avgLabel);
            Paragraph avgVal = new Paragraph(dec(rc.getOverallAverage()) + " / 20", SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 22, Color.WHITE));
            avgVal.setAlignment(Element.ALIGN_CENTER);
            avg.addElement(avgVal);
            String rank = rc.getRank() == null ? null : "Rang : " + rc.getRank() + " / " + str(rc.getClassSize());
            if (rank != null) {
                Paragraph r = new Paragraph(rank, SchoolPdfHeader.font(FontFactory.HELVETICA, 9, Color.WHITE));
                r.setAlignment(Element.ALIGN_CENTER);
                r.setSpacingBefore(3f);
                avg.addElement(r);
            }
            synth.addCell(avg);

            PdfPCell stats = new PdfPCell();
            stats.setBackgroundColor(SchoolPdfHeader.tint(brand, 0.08));
            stats.setBorder(0);
            stats.setPadding(10f);
            stats.addElement(kv("Moyenne de la classe", dec(rc.getClassAverage())));
            stats.addElement(kv("Plus forte moyenne", dec(rc.getHighestAverage())));
            stats.addElement(kv("Plus faible moyenne", dec(rc.getLowestAverage())));
            stats.addElement(kv("Effectif", rc.getClassSize() == null ? "—" : str(rc.getClassSize())));
            synth.addCell(stats);
            doc.add(synth);

            // ── Mention & décision ─────────────────────────────────────────────
            PdfPTable verdict = new PdfPTable(new float[]{1f, 1f});
            verdict.setWidthPercentage(100);
            verdict.setSpacingAfter(6f);
            verdict.addCell(verdictCell("Mention", safe(rc.getHonor(), "—"), brand));
            verdict.addCell(verdictCell("Décision", safe(rc.getDecision(), "—"), brand));
            doc.add(verdict);

            if (rc.getDistinction() != null && !"NONE".equals(rc.getDistinction().name())) {
                Paragraph dist = new Paragraph("Appréciation du conseil de classe : " + distinctionLabel(rc.getDistinction().name()),
                        SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 10, brand));
                dist.setSpacingBefore(2f);
                doc.add(dist);
            }
            if (rc.getComment() != null && !rc.getComment().isBlank()) {
                Paragraph comment = new Paragraph("Appréciation générale : " + rc.getComment(),
                        SchoolPdfHeader.font(FontFactory.HELVETICA, 9.5f, DARK));
                comment.setSpacingBefore(6f);
                doc.add(comment);
            }

            // ── Signatures ─────────────────────────────────────────────────────
            PdfPTable sign = new PdfPTable(new float[]{1f, 1f, 1f});
            sign.setWidthPercentage(100);
            sign.setSpacingBefore(28f);
            signatureCell(sign, "Le Professeur principal");
            signatureCell(sign, "Les Parents / Tuteur");
            signatureCell(sign, "Le Chef d'établissement");
            doc.add(sign);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) {
                doc.close();
            }
            throw e;
        }
    }

    // ── Cell helpers ────────────────────────────────────────────────────────────
    private static void identCell(PdfPTable table, String label, String value, Color brand) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(new Color(225, 228, 232));
        cell.setPadding(8f);
        Paragraph l = new Paragraph(label.toUpperCase(), SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 7.5f, GREY));
        cell.addElement(l);
        Paragraph v = new Paragraph(value, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 11, DARK));
        v.setSpacingBefore(2f);
        cell.addElement(v);
        table.addCell(cell);
    }

    private static void headerCell(PdfPTable table, String text, Color brand) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, Color.WHITE)));
        cell.setBackgroundColor(brand);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6f);
        cell.setBorderColor(brand);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg, boolean multiline) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA, multiline ? 9f : 9.5f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static Paragraph kv(String key, String value) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(key + " : ", SchoolPdfHeader.font(FontFactory.HELVETICA, 9f, GREY)));
        p.add(new Phrase(value, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, DARK)));
        p.setSpacingAfter(3f);
        return p;
    }

    private static PdfPCell verdictCell(String label, String value, Color brand) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(new Color(225, 228, 232));
        cell.setPadding(9f);
        cell.addElement(new Paragraph(label.toUpperCase(), SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 7.5f, GREY)));
        Paragraph v = new Paragraph(value, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 12, brand));
        v.setSpacingBefore(2f);
        cell.addElement(v);
        return cell;
    }

    private static void signatureCell(PdfPTable table, String role) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        cell.setPaddingTop(2f);
        Paragraph line = new Paragraph("_______________________", SchoolPdfHeader.font(FontFactory.HELVETICA, 9, new Color(180, 180, 180)));
        line.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(line);
        Paragraph r = new Paragraph(role, SchoolPdfHeader.font(FontFactory.HELVETICA, 8.5f, GREY));
        r.setAlignment(Element.ALIGN_CENTER);
        r.setSpacingBefore(3f);
        cell.addElement(r);
        table.addCell(cell);
    }

    private static String distinctionLabel(String name) {
        return switch (name) {
            case "FELICITATIONS" -> "Félicitations";
            case "COMPLIMENTS" -> "Compliments";
            case "ENCOURAGEMENTS" -> "Encouragements";
            case "TABLEAU_HONNEUR" -> "Tableau d'honneur";
            case "MISE_EN_GARDE" -> "Mise en garde";
            default -> name;
        };
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    private static String dec(BigDecimal value) {
        return value == null ? "—" : value.toPlainString();
    }

    private static String str(Object value) {
        return value == null ? "—" : value.toString();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
