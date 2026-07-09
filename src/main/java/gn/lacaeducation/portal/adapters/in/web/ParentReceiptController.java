package gn.lacaeducation.portal.adapters.in.web;

import gn.lacaeducation.common.PortalAccess;
import gn.lacaeducation.payments.StudentReceiptProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Téléchargement des reçus par le parent (§11), cloisonné à ses enfants. Placé dans le module portal
 * pour composer payments + students sans créer de cycle entre ces modules.
 */
@Tag(name = "Parent Portal", description = "Téléchargement des reçus d'un enfant (parent)")
@RestController
@RequestMapping("/api/v1/parent/receipts")
@RequiredArgsConstructor
public class ParentReceiptController {

    private final StudentReceiptProvider studentReceiptProvider;
    private final ParentAccessProvider parentAccessProvider;

    @Operation(summary = "Télécharger le PDF d'un reçu d'un enfant du parent connecté")
    @PreAuthorize("hasRole('PARENT')")
    @GetMapping("/{receiptId}/pdf/download")
    public ResponseEntity<byte[]> download(@PathVariable Long receiptId, @AuthenticationPrincipal Jwt jwt) {
        Long studentId = studentReceiptProvider.receiptStudentId(receiptId)
                .orElseThrow(() -> new AccessDeniedException("Reçu introuvable."));
        Long userId;
        try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
        PortalAccess.assertParentCanAccessStudent(jwt.getClaimAsStringList("roles"), userId, studentId,
                parentAccessProvider::isParentOfStudent);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recu-" + receiptId + ".pdf\"")
                .body(studentReceiptProvider.receiptPdfBytes(receiptId));
    }
}
