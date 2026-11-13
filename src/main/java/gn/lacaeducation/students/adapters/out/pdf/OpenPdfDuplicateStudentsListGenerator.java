package gn.lacaeducation.students.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.common.pdf.AdminDocumentLayout;
import gn.lacaeducation.common.pdf.SchoolPdfPageFooter;
import gn.lacaeducation.students.application.port.out.DuplicateStudentsPdfGenerator;
import gn.lacaeducation.students.application.service.DuplicateDetectionService.Candidate;
import gn.lacaeducation.students.application.service.DuplicateDetectionService.Group;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.*;

/**
 * Liste imprimable des groupes de doublons détectés (§retour comptable) — même en-tête que les autres
 * documents administratifs. Une seule page large (A4 paysage) : élève, classe, nom du parent et profil
 * financier tiendraient mal en portrait sans rapetisser le texte au point d'être illisible.
 */
@Component
@RequiredArgsConstructor
class OpenPdfDuplicateStudentsListGenerator implements DuplicateStudentsPdfGenerator {

    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public byte[] generate(Long tenantId, List<Group> groups) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 28, 40);
        try {
            TenantPublicInfo info = tenantInfoProvider.findTenantInfo(tenantId).orElse(null);
            java.awt.Color brand = gn.lacaeducation.common.pdf.SchoolPdfHeader.parseColor(info == null ? null : info.brandColor());
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new SchoolPdfPageFooter(info, brand));
            doc.open();

            int memberCount = groups.stream().mapToInt(g -> g.members().size()).sum();
            AdminDocumentLayout.header(doc, info);
            titleBox(doc, "DOUBLONS ÉLÈVES — " + groups.size() + " GROUPE(S), " + memberCount + " FICHE(S)");

            PdfPTable table = dataTable(new float[]{2.0f, 1.3f, 1.8f, 1.5f, 1.1f, 1.3f, 1.3f, 1.5f});
            headerCell(table, "Élève");
            headerCell(table, "Classe");
            headerCell(table, "Parent");
            headerCell(table, "Téléphone");
            headerCell(table, "Mensualités");
            headerCell(table, "Total facturé");
            headerCell(table, "Total encaissé");
            headerCell(table, "Statut");
            for (Group g : groups) {
                for (Candidate c : g.members()) {
                    cell(table, c.firstName() + " " + c.lastName(), Element.ALIGN_LEFT);
                    cell(table, safe(c.className()), Element.ALIGN_LEFT);
                    cell(table, safe(c.parentFirstName()) + " " + safe(g.parentLastName()), Element.ALIGN_LEFT);
                    cell(table, safe(c.parentPhone()), Element.ALIGN_LEFT);
                    cell(table, String.valueOf(c.monthlyInvoiceCount()), Element.ALIGN_RIGHT);
                    cell(table, money(c.totalInvoiced()) + " GNF", Element.ALIGN_RIGHT);
                    cell(table, money(c.totalPaid()) + " GNF", Element.ALIGN_RIGHT);
                    cell(table, c.suspect() ? "Fiche probablement en trop" : "Historique complet", Element.ALIGN_LEFT);
                }
            }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Échec de génération du PDF des doublons élèves", e);
        }
    }
}
