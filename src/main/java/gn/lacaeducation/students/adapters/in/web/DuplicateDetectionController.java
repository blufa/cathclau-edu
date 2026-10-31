package gn.lacaeducation.students.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.application.service.DuplicateDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Détection de doublons élèves au sein d'une même classe (§retour comptable) — jamais de suppression
 * automatique, voir {@link DuplicateDetectionService}. */
@Tag(name = "Duplicate Detection", description = "Détecte les élèves probablement en double dans une même classe")
@RestController
@RequestMapping("/api/v1/students/duplicates")
@RequiredArgsConstructor
public class DuplicateDetectionController {

    private final DuplicateDetectionService service;

    @Operation(summary = "Groupes d'élèves probablement en double (même classe, même prénom/nom), avec "
            + "le profil financier réel de chacun (factures, encaissé, mensualités facturées) — le champ "
            + "\"suspect\" indique lequel a nettement moins de facturation que les autres du groupe")
    @GetMapping
    // Réservé au SUPER_ADMIN (§retour utilisateur) : mène à une suppression de fiche élève, trop
    // sensible pour le laisser au personnel d'école.
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<DuplicateDetectionService.Group> find() {
        return service.findDuplicateGroups(TenantContext.get());
    }
}
