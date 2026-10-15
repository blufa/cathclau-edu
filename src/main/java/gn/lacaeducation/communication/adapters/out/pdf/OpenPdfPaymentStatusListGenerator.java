package gn.lacaeducation.communication.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.AdminDocumentLayout;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.communication.application.port.out.PaymentStatusListPdfGenerator;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.Candidate;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.*;

/**
 * Liste imprimable des élèves filtrés sur l'écran "Relances paiement" (classe/statut/tranches),
 * même en-tête (logo, établissement, année scolaire) que les autres documents officiels — demande
 * utilisateur explicite ("il faut toujours reproduire les mêmes entêtes de la facture"). Une seule
 * page large (A4 paysage) : la table a 6 colonnes et une liste de plusieurs dizaines d'élèves tiendrait
 * mal en portrait sans rapetisser le texte au point d'être illisible.
 */
@Component
@RequiredArgsConstructor
class OpenPdfPaymentStatusListGenerator implements PaymentStatusListPdfGenerator {

    private static final Map<Status, String> STATUS_LABEL = Map.of(
            Status.OVERDUE, "En retard",
            Status.UPCOMING, "Échéance proche",
            Status.UP_TO_DATE, "À jour");

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(Long tenantId, List<Candidate> candidates) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 28, 40);
        try {
            TenantPublicInfo info = tenantInfoProvider.findTenantInfo(tenantId).orElse(null);
            java.awt.Color brand = gn.lacaeducation.common.pdf.SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            AdminDocumentLayout.header(doc, info);
            titleBox(doc, "SUIVI DES PAIEMENTS — " + candidates.size() + " ÉLÈVE(S)");

            PdfPTable table = dataTable(new float[]{2.4f, 1.6f, 1.3f, 1f, 1.4f, 1.2f});
            headerCell(table, "Élève");
            headerCell(table, "Classe");
            headerCell(table, "Statut");
            headerCell(table, "Jours");
            headerCell(table, "Montant dû");
            headerCell(table, "Tranches");
            for (Candidate c : candidates) {
                cell(table, c.studentName(), Element.ALIGN_LEFT);
                cell(table, c.className(), Element.ALIGN_LEFT);
                cell(table, STATUS_LABEL.get(c.status()), Element.ALIGN_LEFT);
                cell(table, c.status() == Status.UP_TO_DATE ? "-" : String.valueOf(Math.abs(c.daysLate())), Element.ALIGN_RIGHT);
                cell(table, c.amountDue() != null && c.amountDue().signum() > 0 ? money(c.amountDue()) + " GNF" : "-", Element.ALIGN_RIGHT);
                cell(table, c.totalTranches() > 0 ? c.tranchesPaid() + "/" + c.totalTranches() : "-", Element.ALIGN_RIGHT);
            }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Échec de génération du PDF de la liste de paiements", e);
        }
    }
}
