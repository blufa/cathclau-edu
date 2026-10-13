package gn.lacaeducation.students.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.application.service.DataIntegrityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Vérification/correction de la cohérence des données du tenant impersonné (§retour utilisateur,
 * suite à des suppressions manuelles en base hors de l'application) — réservé SUPER_ADMIN, voir
 * {@link DataIntegrityService} pour le détail des écarts détectés. */
@Tag(name = "Data Integrity", description = "Vérification/correction de la cohérence des données (effectifs, factures/paiements orphelins)")
@RestController
@RequestMapping("/api/v1/students/data-integrity")
@RequiredArgsConstructor
public class DataIntegrityController {

    private final DataIntegrityService service;

    @Operation(summary = "Scanner le tenant courant à la recherche d'écarts (effectifs, factures/paiements orphelins)")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/scan")
    public DataIntegrityService.ScanResult scan() {
        return service.scan(TenantContext.get());
    }

    @Operation(summary = "Recalculer l'effectif réel d'une classe et l'enregistrer")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/headcount/{classId}/fix")
    public java.util.Map<String, Integer> fixHeadcount(@PathVariable Long classId) {
        return java.util.Map.of("headcount", service.fixHeadcount(classId));
    }

    @Operation(summary = "Supprimer une facture orpheline (élève déjà supprimé) et ses lignes liées")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/invoices/{invoiceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrphanedInvoice(@PathVariable Long invoiceId) {
        service.deleteOrphanedInvoice(invoiceId);
    }

    @Operation(summary = "Supprimer un paiement orphelin (élève déjà supprimé) et ses lignes liées")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/payments/{paymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrphanedPayment(@PathVariable Long paymentId) {
        service.deleteOrphanedPayment(paymentId);
    }

    @Operation(summary = "Supprimer toutes les lignes orphelines d'une table pour un ancien élève")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/tables/{table}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrphanedRows(@PathVariable String table, @PathVariable Long studentId) {
        service.deleteOrphanedRows(table, studentId);
    }
}
