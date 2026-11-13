package gn.lacaeducation.students.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.application.port.out.DuplicateStudentsPdfGenerator;
import gn.lacaeducation.students.application.service.DuplicateDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Détection de doublons élèves — même prénom, même nom, et même nom de famille d'un parent (§retour
 * comptable, plus limité à la même classe) — jamais de suppression automatique, voir
 * {@link DuplicateDetectionService}. */
@Tag(name = "Duplicate Detection", description = "Détecte les élèves probablement en double (même prénom/nom/nom de famille du parent)")
@RestController
@RequestMapping("/api/v1/students/duplicates")
@RequiredArgsConstructor
public class DuplicateDetectionController {

    private final DuplicateDetectionService service;
    private final DuplicateStudentsPdfGenerator pdfGenerator;

    @Operation(summary = "Groupes d'élèves probablement en double (même prénom/nom élève et même nom de "
            + "famille d'un parent), avec le profil financier réel de chacun (factures, encaissé, "
            + "mensualités facturées) — le champ \"suspect\" indique lequel a nettement moins de "
            + "facturation que les autres du groupe")
    @GetMapping
    // Réservé au SUPER_ADMIN (§retour utilisateur) : mène à une suppression de fiche élève, trop
    // sensible pour le laisser au personnel d'école.
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<DuplicateDetectionService.Group> find() {
        return service.findDuplicateGroups(TenantContext.get());
    }

    @Operation(summary = "PDF imprimable de tous les groupes de doublons détectés actuellement")
    @GetMapping(value = "/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public org.springframework.http.ResponseEntity<byte[]> printPdf() {
        Long tenantId = TenantContext.get();
        byte[] pdf = pdfGenerator.generate(tenantId, service.findDuplicateGroups(tenantId));
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=doublons-eleves.pdf")
                .body(pdf);
    }
}
