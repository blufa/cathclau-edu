package gn.lacaeducation.portal.adapters.in.web;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.PortalAccess;
import gn.lacaeducation.grading.StudentReportProvider;
import gn.lacaeducation.payments.StudentReceiptProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Documents téléchargeables d'un enfant pour le portail parent (§11) : bulletins publiés et reçus,
 * exposés sous forme de liens vers les endpoints de téléchargement sécurisés. Strictement scopé aux
 * enfants rattachés au parent connecté.
 */
@Tag(name = "Parent Portal", description = "Documents téléchargeables d'un enfant (lecture seule, scopé au parent)")
@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
public class ParentDocumentsController {

    private final ParentAccessProvider parentAccessProvider;
    private final StudentReportProvider studentReportProvider;
    private final StudentReceiptProvider studentReceiptProvider;
    private final AcademicsDataProvider academicsDataProvider;

    /** Un document téléchargeable : type (BULLETIN, RECU), libellé et URL de téléchargement. */
    public record DocumentItem(String type, Long id, String label, String downloadUrl) {}

    @Operation(summary = "Documents téléchargeables d'un enfant : bulletins publiés et reçus")
    @PreAuthorize("hasRole('PARENT')")
    @GetMapping("/children/{studentId}/documents")
    public List<DocumentItem> documents(@PathVariable Long studentId, @AuthenticationPrincipal Jwt jwt) {
        assertParentScope(jwt, studentId);

        List<DocumentItem> documents = new ArrayList<>();

        studentReportProvider.listPublished(studentId).forEach(report -> {
            String period = academicsDataProvider.findPeriod(report.periodId())
                    .map(p -> p.label()).orElse("période");
            documents.add(new DocumentItem("BULLETIN", report.reportCardId(), "Bulletin — " + period,
                    "/api/v1/grading/report-cards/" + report.reportCardId() + "/pdf/download"));
        });

        studentReceiptProvider.listReceipts(studentId).forEach(receipt ->
                documents.add(new DocumentItem("RECU", receipt.receiptId(), "Reçu " + receipt.reference(),
                        "/api/v1/parent/receipts/" + receipt.receiptId() + "/pdf/download")));

        return documents;
    }

    private void assertParentScope(Jwt jwt, Long studentId) {
        if (jwt == null) return;
        Long userId;
        try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
        PortalAccess.assertParentCanAccessStudent(jwt.getClaimAsStringList("roles"), userId, studentId,
                parentAccessProvider::isParentOfStudent);
    }
}
