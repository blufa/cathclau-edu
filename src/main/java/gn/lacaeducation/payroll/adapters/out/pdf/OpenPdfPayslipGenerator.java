package gn.lacaeducation.payroll.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator;
import gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator.TeacherHours;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipLine;
import gn.lacaeducation.payroll.domain.PayslipLineType;
import gn.lacaeducation.payroll.domain.PayslipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.*;

/**
 * Rend une fiche de paie en PDF A5, sobre (noir sur blanc) — même mise en page que le reçu de
 * paiement et la facture (voir {@code gn.lacaeducation.common.pdf.AdminDocumentLayout}) : en-tête
 * établissement, titre encadré, bloc enseignant, tableau des gains/retenues, net à payer et cadre
 * de signature.
 */
@Component
@RequiredArgsConstructor
class OpenPdfPayslipGenerator implements PayslipPdfGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(Payslip p, List<PayslipLine> lines, TeacherHours hours) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 28, 28, 24, 36);
        try {
            TenantPublicInfo info = p.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(p.getTenantId()).orElse(null);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info == null ? null : info.schoolName(),
                    SchoolPdfHeader.parseColor(info == null ? null : info.brandColor())));
            doc.open();

            header(doc, info);
            titleBox(doc, "FICHE DE PAIE");

            PdfPTable idBlock = infoTable();
            row(idBlock, "Enseignant", p.getTeacherName() != null ? p.getTeacherName() : "#" + p.getTeacherId());
            row(idBlock, "Période", safe(p.getPeriodLabel()));
            if (p.getPeriodStart() != null || p.getPeriodEnd() != null) {
                row(idBlock, "Du / Au", (p.getPeriodStart() == null ? "-" : p.getPeriodStart().format(DATE))
                        + "  →  " + (p.getPeriodEnd() == null ? "-" : p.getPeriodEnd().format(DATE)));
            }
            row(idBlock, "Statut", statusLabel(p.getStatus()));
            if (p.getStatus() == PayslipStatus.PAID && p.getPaymentMethod() != null) {
                row(idBlock, "Mode de paiement", p.getPaymentMethod());
            }
            doc.add(idBlock);

            if (hours != null) {
                PdfPTable hoursBlock = infoTable();
                hoursBlock.setSpacingBefore(6f);
                row(hoursBlock, "Heures dues (mois)", hoursLabel(hours.dueMinutes()));
                row(hoursBlock, "Heures réalisées", hoursLabel(hours.realizedMinutes()));
                row(hoursBlock, "Heures non réalisées", hoursLabel(hours.missingMinutes()));
                doc.add(hoursBlock);
            }

            PdfPTable amounts = dataTable(new float[]{2f, 1.5f});
            headerCell(amounts, "Élément");
            headerCell(amounts, "Montant");
            cell(amounts, "Salaire de base", Element.ALIGN_LEFT);
            cell(amounts, money(p.getBaseSalary()) + " GNF", Element.ALIGN_RIGHT);
            for (PayslipLine l : lines) {
                if (l.getLineType() == PayslipLineType.EARNING) {
                    cell(amounts, l.getLabel(), Element.ALIGN_LEFT);
                    cell(amounts, "+ " + money(l.getAmount()) + " GNF", Element.ALIGN_RIGHT);
                }
            }
            cell(amounts, "Salaire brut", Element.ALIGN_LEFT);
            cell(amounts, money(p.getGrossSalary()) + " GNF", Element.ALIGN_RIGHT);
            for (PayslipLine l : lines) {
                if (l.getLineType() == PayslipLineType.DEDUCTION) {
                    cell(amounts, l.getLabel(), Element.ALIGN_LEFT);
                    cell(amounts, "− " + money(l.getAmount()) + " GNF", Element.ALIGN_RIGHT);
                }
            }
            doc.add(amounts);

            totalRow(doc, "Net à payer", money(p.getNetSalary()) + " GNF");

            signatureBox(doc, "Signature et Cachet de l'Établissement");

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Échec de génération du PDF de la fiche de paie " + p.getId(), e);
        }
    }

    private static String hoursLabel(int minutes) {
        return (minutes / 60) + "h" + String.format("%02d", minutes % 60);
    }

    private static String statusLabel(PayslipStatus s) {
        if (s == null) return "-";
        return switch (s) {
            case DRAFT -> "Brouillon";
            case VALIDATED -> "Validée";
            case PAID -> "Payée";
        };
    }
}
