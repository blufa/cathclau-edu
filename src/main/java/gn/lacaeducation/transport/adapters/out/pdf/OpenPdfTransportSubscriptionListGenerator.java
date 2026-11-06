package gn.lacaeducation.transport.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.AdminDocumentLayout;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.transport.application.port.out.TransportSubscriptionListPdfGenerator;
import gn.lacaeducation.transport.application.service.TransportService.SubscriptionPrintRow;
import gn.lacaeducation.transport.domain.SubscriptionStatus;
import gn.lacaeducation.transport.domain.TransportDirection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.*;

/**
 * Liste imprimable des abonnés transport filtrés sur l'écran (ligne/mois payé), même en-tête que les
 * autres documents officiels — §retour comptable : "ajouter la classe des élèves, et qu'il puisse
 * l'imprimer" sur la liste des transports.
 */
@Component
@RequiredArgsConstructor
class OpenPdfTransportSubscriptionListGenerator implements TransportSubscriptionListPdfGenerator {

    private static final Map<TransportDirection, String> DIRECTION_LABEL = Map.of(
            TransportDirection.PICKUP, "Ramassage",
            TransportDirection.DROP, "Dépose",
            TransportDirection.BOTH, "Aller-retour");

    private static final Map<SubscriptionStatus, String> STATUS_LABEL = Map.of(
            SubscriptionStatus.ACTIVE, "Actif",
            SubscriptionStatus.CANCELLED, "Résilié");

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(Long tenantId, List<SubscriptionPrintRow> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 28, 40);
        try {
            TenantPublicInfo info = tenantInfoProvider.findTenantInfo(tenantId).orElse(null);
            java.awt.Color brand = gn.lacaeducation.common.pdf.SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            AdminDocumentLayout.header(doc, info);
            titleBox(doc, "LISTE DES ABONNÉS TRANSPORT — " + rows.size() + " ÉLÈVE(S)");

            PdfPTable table = dataTable(new float[]{1.6f, 2.4f, 1.6f, 1.8f, 1.3f, 1.1f, 1.2f});
            headerCell(table, "Matricule");
            headerCell(table, "Élève");
            headerCell(table, "Classe");
            headerCell(table, "Ligne");
            headerCell(table, "Sens");
            headerCell(table, "Statut");
            headerCell(table, "Mois payés");
            for (SubscriptionPrintRow r : rows) {
                cell(table, r.studentNumber() != null ? r.studentNumber() : "-", Element.ALIGN_LEFT);
                cell(table, r.studentName(), Element.ALIGN_LEFT);
                cell(table, r.className() != null ? r.className() : "-", Element.ALIGN_LEFT);
                cell(table, r.routeName(), Element.ALIGN_LEFT);
                cell(table, DIRECTION_LABEL.get(r.direction()), Element.ALIGN_LEFT);
                cell(table, STATUS_LABEL.get(r.status()), Element.ALIGN_LEFT);
                cell(table, String.valueOf(r.paidMonthsCount()), Element.ALIGN_RIGHT);
            }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Échec de génération du PDF de la liste transport", e);
        }
    }
}
