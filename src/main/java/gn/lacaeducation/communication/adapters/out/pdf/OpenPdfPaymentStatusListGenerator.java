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
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.ParentContact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static gn.lacaeducation.common.pdf.AdminDocumentLayout.*;

/**
 * Liste imprimable des élèves filtrés sur l'écran "Relances paiement" (classe/statut/tranches),
 * même en-tête (logo, établissement, année scolaire) que les autres documents officiels — demande
 * utilisateur explicite ("il faut toujours reproduire les mêmes entêtes de la facture"). Une seule
 * page large (A4 paysage) : la table a plusieurs colonnes et une liste de plusieurs dizaines d'élèves
 * tiendrait mal en portrait sans rapetisser le texte au point d'être illisible.
 *
 * §retour comptable : colonnes Statut/Jours/Montant dû retirées, "Tranches" remplacée par le nombre de
 * mois payés (compréhensible sans contexte supplémentaire), Matricule et Parent ajoutés.
 */
@Component
@RequiredArgsConstructor
class OpenPdfPaymentStatusListGenerator implements PaymentStatusListPdfGenerator {

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

            // "Mois payés" élargie (1.6f → 3f) : affiche désormais la liste des mois (ex. "Octobre,
            // Novembre"), pas un simple compte — §retour comptable : "il faut mettre ces mois".
            PdfPTable table = dataTable(new float[]{1.2f, 2.2f, 1.3f, 3f, 2.1f});
            headerCell(table, "Matricule");
            headerCell(table, "Élève");
            headerCell(table, "Classe");
            headerCell(table, "Mois payés");
            headerCell(table, "Parent");
            for (Candidate c : candidates) {
                cell(table, c.studentNumber(), Element.ALIGN_LEFT);
                cell(table, c.studentName(), Element.ALIGN_LEFT);
                cell(table, c.className(), Element.ALIGN_LEFT);
                cell(table, c.paidMonths().isEmpty() ? "-" : String.join(", ", c.paidMonths()), Element.ALIGN_LEFT);
                cell(table, c.parents().isEmpty() ? "-"
                        : c.parents().stream().map(ParentContact::name).collect(java.util.stream.Collectors.joining(", ")),
                        Element.ALIGN_LEFT);
            }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Échec de génération du PDF de la liste de paiements", e);
        }
    }
}
