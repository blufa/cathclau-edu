package gn.lacaeducation.payroll.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.SchoolPdfHeader;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.payroll.application.port.out.SalaryAdvanceVoucherPdfGenerator;
import gn.lacaeducation.payroll.domain.SalaryAdvance;
import gn.lacaeducation.payroll.domain.SalaryAdvanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.*;

/**
 * Rend le bon d'avance sur salaire en PDF A5, sobre — même mise en page que la fiche de paie/le reçu
 * de paiement (voir {@code gn.lacaeducation.common.pdf.AdminDocumentLayout}) : remis au salarié au
 * moment de l'octroi, distinct de la fiche de paie mensuelle.
 */
@Component
@RequiredArgsConstructor
class OpenPdfSalaryAdvanceVoucherGenerator implements SalaryAdvanceVoucherPdfGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(SalaryAdvance a) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 28, 28, 24, 36);
        try {
            TenantPublicInfo info = a.getTenantId() == null ? null
                    : tenantInfoProvider.findTenantInfo(a.getTenantId()).orElse(null);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info,
                    SchoolPdfHeader.parseColor(info == null ? null : info.brandColor())));
            doc.open();

            header(doc, info);
            titleBox(doc, "BON D'AVANCE SUR SALAIRE");

            PdfPTable idBlock = infoTable();
            row(idBlock, "Salarié", a.getTeacherName() != null ? a.getTeacherName() : "#" + a.getTeacherId());
            row(idBlock, "Date", a.getGrantedAt() != null ? a.getGrantedAt().toLocalDate().format(DATE) : "-");
            row(idBlock, "Motif", safe(a.getReason()));
            row(idBlock, "Statut", statusLabel(a.getStatus()));
            doc.add(idBlock);

            totalRow(doc, "Montant avancé", money(a.getAmount()) + " GNF");

            PdfPTable balance = infoTable();
            balance.setSpacingBefore(6f);
            row(balance, "Solde restant à déduire", money(a.getRemainingBalance()) + " GNF");
            doc.add(balance);

            signatureBox(doc, "Signature et Cachet de l'Établissement");

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Échec de génération du bon d'avance sur salaire " + a.getId(), e);
        }
    }

    private static String statusLabel(SalaryAdvanceStatus s) {
        if (s == null) return "-";
        return switch (s) {
            case ACTIVE -> "En cours";
            case REPAID -> "Soldée";
            case CANCELLED -> "Annulée";
        };
    }
}
