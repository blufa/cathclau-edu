package gn.lacaeducation.billing.adapters.in.web;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.common.PortalAccess;
import gn.lacaeducation.students.ParentAccessProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
@Tag(name = "Invoices", description = "Gérer les factures des élèves pour les frais scolaires et suivre le statut de paiement")
@RestController @RequestMapping("/api/v1/billing/invoices") @RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceUseCase useCase;
    private final ParentAccessProvider parentAccessProvider;

    private void assertParentScope(Jwt jwt, Long studentId) {
        if (jwt == null) return;
        Long userId;
        try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
        PortalAccess.assertParentCanAccessStudent(jwt.getClaimAsStringList("roles"), userId, studentId,
                parentAccessProvider::isParentOfStudent);
    }
    public record CreateRequest(Long tenantId, Long studentId, Long feeId, String reference, BigDecimal totalAmount, Long academicYearId) {}
    public record PaymentRequest(BigDecimal amount) {}
    public record DiscountRequest(BigDecimal amount, String reason) {}
    public record BulkGenerateRequest(Long tenantId, Long classId, Long feeId, Long academicYearId) {}
    public record GenerateMonthlyRequest(Long tenantId, Long studentId, Long feeId, Long academicYearId) {}
    @Operation(summary = "Créer une nouvelle facture pour un élève")
    @PreAuthorize("hasAuthority('facture:create')")
    @PostMapping public ResponseEntity<Invoice> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new InvoiceUseCase.CreateInvoiceCommand(r.tenantId(), r.studentId(), r.feeId(), r.reference(), r.totalAmount(), r.academicYearId())));
    }
    @Operation(summary = "Générer en masse les factures de tous les élèves inscrits dans une classe")
    @PreAuthorize("hasAuthority('facture:create')")
    @PostMapping("/generate-bulk") public ResponseEntity<List<Invoice>> generateBulk(@RequestBody BulkGenerateRequest r) {
        return ResponseEntity.ok(useCase.generateBulk(new InvoiceUseCase.BulkGenerateCommand(r.tenantId(), r.classId(), r.feeId(), r.academicYearId())));
    }
    @Operation(summary = "Générer une facture par mois (échéance) d'un frais MENSUEL pour un élève")
    @PreAuthorize("hasAuthority('facture:create')")
    @PostMapping("/generate-monthly") public ResponseEntity<List<Invoice>> generateMonthly(@RequestBody GenerateMonthlyRequest r) {
        return ResponseEntity.ok(useCase.generateMonthly(new InvoiceUseCase.GenerateMonthlyCommand(r.tenantId(), r.studentId(), r.feeId(), r.academicYearId())));
    }
    @Operation(summary = "Consulter une facture par son ID")
    @GetMapping("/{id}") public ResponseEntity<Invoice> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Récupérer l'URL du PDF de la facture — généré à la demande si absent")
    @GetMapping("/{id}/pdf") public ResponseEntity<java.util.Map<String, String>> pdf(@PathVariable Long id) {
        if (useCase.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(java.util.Map.of("url", useCase.pdfUrl(id)));
    }

    @Operation(summary = "Diffuser les octets du PDF de la facture (inline) — indépendant de la livraison Cloudinary")
    @GetMapping("/{id}/pdf/download") public ResponseEntity<byte[]> pdfDownload(@PathVariable Long id) {
        if (useCase.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"facture-" + id + ".pdf\"")
                .body(useCase.pdfBytes(id));
    }
    @Operation(summary = "Consulter une facture par son numéro de référence")
    @GetMapping("/reference/{reference}") public ResponseEntity<Invoice> findByReference(@PathVariable String reference) {
        return useCase.findByReference(reference).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Lister les factures d'un élève")
    @GetMapping("/student/{studentId}") public List<Invoice> findByStudent(@PathVariable Long studentId,
            @AuthenticationPrincipal Jwt jwt) { assertParentScope(jwt, studentId); return useCase.findByStudentId(studentId); }
    @Operation(summary = "Lister les factures d'un établissement — passer academicYearId pour limiter à une année scolaire (recommandé : un appel sans filtre mélange toutes les années créées)")
    @GetMapping("/tenant/{tenantId}") public List<Invoice> findByTenant(@PathVariable Long tenantId, @RequestParam(required = false) Long academicYearId) {
        return academicYearId != null ? useCase.findByTenantIdAndAcademicYearId(tenantId, academicYearId) : useCase.findByTenantId(tenantId);
    }
    @Operation(summary = "Lister les factures d'un établissement filtrées par statut de paiement")
    @GetMapping("/tenant/{tenantId}/status/{status}") public List<Invoice> findByStatus(@PathVariable Long tenantId, @PathVariable InvoiceStatus status) { return useCase.findByTenantIdAndStatus(tenantId, status); }
    @Operation(summary = "Appliquer un montant de paiement à une facture")
    @PreAuthorize("hasAuthority('paiement:create')")
    @PutMapping("/{id}/payment") public ResponseEntity<Invoice> applyPayment(@PathVariable Long id, @RequestBody PaymentRequest r) { return ResponseEntity.ok(useCase.applyPayment(id, r.amount())); }
    @Operation(summary = "Appliquer une remise / bourse à une facture (recalcule le reste à payer)")
    @PreAuthorize("hasAuthority('facture:create')")
    @PatchMapping("/{id}/discount") public ResponseEntity<Invoice> applyDiscount(@PathVariable Long id, @RequestBody DiscountRequest r) { return ResponseEntity.ok(useCase.applyDiscount(id, r.amount(), r.reason())); }
    @Operation(summary = "Appliquer la remise pour paiement annuel complet sur les factures restantes d'un frais (§billing) — "
            + "à appeler juste avant un paiement groupé couvrant toutes les tranches restantes")
    @PreAuthorize("hasAuthority('facture:create')")
    @PostMapping("/early-payment-discount")
    public List<Invoice> applyEarlyPaymentDiscount(@RequestParam Long studentId, @RequestParam Long feeId) {
        return useCase.applyEarlyPaymentDiscount(studentId, feeId);
    }
    @Operation(summary = "Annuler la remise pour paiement annuel complet précédemment appliquée (§billing) — "
            + "à appeler dès que la sélection ne couvre plus toutes les tranches restantes")
    @PreAuthorize("hasAuthority('facture:create')")
    @DeleteMapping("/early-payment-discount")
    public List<Invoice> removeEarlyPaymentDiscount(@RequestParam Long studentId, @RequestParam Long feeId) {
        return useCase.removeEarlyPaymentDiscount(studentId, feeId);
    }
    @Operation(summary = "Avancer le recouvrement d'une étape (relance 1 → relance 2 → exclusion)")
    @PreAuthorize("hasAuthority('facture:create')")
    @PatchMapping("/{id}/recovery/advance") public ResponseEntity<Invoice> advanceRecovery(@PathVariable Long id) { return ResponseEntity.ok(useCase.advanceRecovery(id)); }
    @Operation(summary = "Positionner l'étape de recouvrement")
    @PreAuthorize("hasAuthority('facture:create')")
    @PatchMapping("/{id}/recovery") public ResponseEntity<Invoice> setRecovery(@PathVariable Long id, @RequestParam gn.lacaeducation.billing.domain.RecoveryStage stage) { return ResponseEntity.ok(useCase.setRecoveryStage(id, stage)); }
    @Operation(summary = "Annuler une facture")
    @PreAuthorize("hasAuthority('facture:create')")
    @DeleteMapping("/{id}/cancel") public ResponseEntity<Void> cancel(@PathVariable Long id) { useCase.cancel(id); return ResponseEntity.noContent().build(); }
    @Operation(summary = "Supprimer une facture")
    @PreAuthorize("hasAuthority('facture:delete')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
