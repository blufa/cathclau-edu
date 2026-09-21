package gn.lacaeducation.attendance.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.attendance.application.port.out.AttendanceReportPdfGenerator;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Renders attendance documents (absentee list, lateness report) to PDF using OpenPDF. */
@Component
@RequiredArgsConstructor
class OpenPdfAttendanceReportGenerator implements AttendanceReportPdfGenerator {

    private static final Color GREY = SchoolPdfHeader.GREY;
    private static final Color DARK = SchoolPdfHeader.DARK;
    private static final Color ZEBRA = new Color(247, 248, 250);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] absenteeList(AbsenteeList data) {
        return render(data.tenantId(), "LISTE DES ABSENTS",
                join(" · ", data.className(), data.date() == null ? null : "Journée du " + data.date().format(DATE)),
                new String[]{"Élève", "Statut", "Justifié", "Motif"},
                new float[]{3f, 1.3f, 1.2f, 3f},
                data.rows().size() + " élève(s) concerné(s)",
                (table, brand) -> {
                    boolean zebra = false;
                    for (AbsenteeRow r : data.rows()) {
                        Color bg = zebra ? ZEBRA : Color.WHITE; zebra = !zebra;
                        bodyCell(table, safe(r.studentName()), Element.ALIGN_LEFT, bg);
                        bodyCell(table, safe(r.status()), Element.ALIGN_CENTER, bg);
                        bodyCell(table, r.justified() ? "Oui" : "Non", Element.ALIGN_CENTER, bg);
                        bodyCell(table, safe(r.reason()), Element.ALIGN_LEFT, bg);
                    }
                    if (data.rows().isEmpty()) emptyRow(table, 4, "Aucun absent ni retard enregistré pour cette date.");
                });
    }

    @Override
    public byte[] latenessReport(LatenessReport data) {
        String period = data.from() == null || data.to() == null ? null
                : "Du " + data.from().format(DATE) + " au " + data.to().format(DATE);
        int total = data.rows().stream().mapToInt(LatenessRow::count).sum();
        return render(data.tenantId(), "RAPPORT DES RETARDS",
                join(" · ", data.className(), period),
                new String[]{"Élève", "Nb de retards", "Dates"},
                new float[]{3f, 1.6f, 4f},
                total + " retard(s) sur la période — " + data.rows().size() + " élève(s)",
                (table, brand) -> {
                    boolean zebra = false;
                    for (LatenessRow r : data.rows()) {
                        Color bg = zebra ? ZEBRA : Color.WHITE; zebra = !zebra;
                        bodyCell(table, safe(r.studentName()), Element.ALIGN_LEFT, bg);
                        bodyCell(table, String.valueOf(r.count()), Element.ALIGN_CENTER, bg);
                        bodyCell(table, safe(r.dates()), Element.ALIGN_LEFT, bg);
                    }
                    if (data.rows().isEmpty()) emptyRow(table, 3, "Aucun retard enregistré sur la période.");
                });
    }

    @Override
    public byte[] teacherLatenessReport(TeacherLatenessReport data) {
        String period = data.from() == null || data.to() == null ? null
                : "Du " + data.from().format(DATE) + " au " + data.to().format(DATE);
        int totalLate = data.rows().stream().mapToInt(TeacherReportRow::lateMinutes).sum();
        int totalEarly = data.rows().stream().mapToInt(TeacherReportRow::earlyMinutes).sum();
        long absentDays = data.rows().stream().filter(TeacherReportRow::absent).count();
        String summary = totalLate + " min de retard, " + totalEarly + " min de départ anticipé, "
                + absentDays + " jour(s) d'absence sur la période";
        return render(data.tenantId(), "RÉCAPITULATIF RETARDS / ABSENCES",
                join(" · ", data.teacherName(), period),
                new String[]{"Date", "Matière", "Classe", "Retard", "Départ anticipé", "Absence"},
                new float[]{1.6f, 2.2f, 1.6f, 1.2f, 1.6f, 2f},
                summary,
                (table, brand) -> {
                    boolean zebra = false;
                    for (TeacherReportRow r : data.rows()) {
                        Color bg = zebra ? ZEBRA : Color.WHITE; zebra = !zebra;
                        bodyCell(table, r.date() == null ? "—" : r.date().format(DATE), Element.ALIGN_LEFT, bg);
                        bodyCell(table, r.absent() ? "—" : safe(r.subjectLabel()), Element.ALIGN_LEFT, bg);
                        bodyCell(table, r.absent() ? "—" : safe(r.className()), Element.ALIGN_LEFT, bg);
                        bodyCell(table, r.absent() || r.lateMinutes() <= 0 ? "—" : r.lateMinutes() + " min", Element.ALIGN_CENTER, bg);
                        bodyCell(table, r.absent() || r.earlyMinutes() <= 0 ? "—" : r.earlyMinutes() + " min", Element.ALIGN_CENTER, bg);
                        bodyCell(table, r.absent() ? safe(r.absenceReason()) : "—", Element.ALIGN_LEFT, bg);
                    }
                    if (data.rows().isEmpty()) emptyRow(table, 6, "Aucun retard, départ anticipé ni absence sur la période.");
                });
    }

    // ── Rendu commun ────────────────────────────────────────────────────────────
    private interface TableFiller { void fill(PdfPTable table, Color brand); }

    private byte[] render(Long tenantId, String title, String subtitle, String[] headers, float[] widths,
                          String summary, TableFiller filler) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 54);
        try {
            TenantPublicInfo info = tenantId == null ? null : tenantInfoProvider.findTenantInfo(tenantId).orElse(null);
            Color brand = SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            SchoolPdfHeader.render(doc, tenantInfoProvider, tenantId, title);

            Paragraph t = new Paragraph(title, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 16, brand));
            t.setAlignment(Element.ALIGN_CENTER);
            t.setSpacingBefore(8f);
            doc.add(t);
            if (subtitle != null && !subtitle.isBlank()) {
                Paragraph s = new Paragraph(subtitle, SchoolPdfHeader.font(FontFactory.HELVETICA, 10, GREY));
                s.setAlignment(Element.ALIGN_CENTER);
                s.setSpacingAfter(4f);
                doc.add(s);
            }
            Paragraph sum = new Paragraph(summary, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, DARK));
            sum.setAlignment(Element.ALIGN_CENTER);
            sum.setSpacingAfter(12f);
            doc.add(sum);

            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(100);
            for (String h : headers) headerCell(table, h, brand);
            filler.fill(table, brand);
            doc.add(table);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            if (doc.isOpen()) doc.close();
            throw e;
        }
    }

    private static void headerCell(PdfPTable table, String text, Color brand) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_BOLD, 9.5f, Color.WHITE)));
        cell.setBackgroundColor(brand);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6f);
        cell.setBorderColor(brand);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA, 9.5f, DARK)));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        cell.setBorderColor(new Color(232, 234, 237));
        table.addCell(cell);
    }

    private static void emptyRow(PdfPTable table, int colspan, String text) {
        PdfPCell empty = new PdfPCell(new Phrase(text, SchoolPdfHeader.font(FontFactory.HELVETICA_OBLIQUE, 9.5f, GREY)));
        empty.setColspan(colspan);
        empty.setHorizontalAlignment(Element.ALIGN_CENTER);
        empty.setPadding(12f);
        table.addCell(empty);
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

    private static String safe(String s) { return s == null || s.isBlank() ? "—" : s; }
}
