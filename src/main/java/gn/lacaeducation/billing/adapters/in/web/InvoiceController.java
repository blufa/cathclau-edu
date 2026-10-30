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
    public record GenerateSelectedInstallmentsRequest(Long tenantId, Long studentId, Long feeId, Long academicYearId, List<Long> installmentIds) {}
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
    @PostMapping("/generate-monthly") public ResponseEntity<InvoiceUseCase.GenerateMonthlyResult> generateMonthly(@RequestBody GenerateMonthlyRequest r) {
        return ResponseEntity.ok(useCase.generateMonthly(new InvoiceUseCase.GenerateMonthlyCommand(r.tenantId(), r.studentId(), r.feeId(), r.academicYearId())));
    }
    @Operation(summary = "Générer uniquement les échéances sélectionnées (un ou plusieurs mois choisis) — transport facturé au mois, à la demande")
    @PreAuthorize("hasAuthority('facture:create')")
    @PostMapping("/generate-selected-installments") public ResponseEntity<InvoiceUseCase.GenerateMonthlyResult> generateSelectedInstallments(@RequestBody GenerateSelectedInstallmentsRequest r) {
        return ResponseEntity.ok(useCase.generateSelectedInstallments(
                new InvoiceUseCase.GenerateMonthlyCommand(r.tenantId(), r.studentId(), r.feeId(), r.academicYearId()), r.installmentIds()));
    }
    @Operation(summary = "Consulter une facture par son ID")
    @PreAuthorize("hasAuthority('facture:read') or hasRole('PARENT') or hasRole('STUDENT')")
    @GetMapping("/{id}") public ResponseEntity<Invoice> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Récupérer l'URL du PDF de la facture — généré à la demande si absent")
    @PreAuthorize("hasAuthority('facture:read') or hasRole('PARENT') or hasRole('STUDENT')")
    @GetMapping("/{id}/pdf") public ResponseEntity<java.util.Map<String, String>> pdf(@PathVariable Long id) {
        if (useCase.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(java.util.Map.of("url", useCase.pdfUrl(id)));
    }

    @Operation(summary = "Diffuser les octets du PDF de la facture (inline) — indépendant de la livraison Cloudinary")
    @PreAuthorize("hasAuthority('facture:read') or hasRole('PARENT') or hasRole('STUDENT')")
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
    @PreAuthorize("hasAuthority('facture:read') or hasRole('PARENT') or hasRole('STUDENT')")
    @GetMapping("/reference/{reference}") public ResponseEntity<Invoice> findByReference(@PathVariable String reference) {
        return useCase.findByReference(reference).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Lister les factures d'un élève")
    @PreAuthorize("hasAuthority('facture:read') or hasRole('PARENT') or hasRole('STUDENT')")
    @GetMapping("/student/{studentId}") public List<Invoice> findByStudent(@PathVariable Long studentId,
            @AuthenticationPrincipal Jwt jwt) { assertParentScope(jwt, studentId); return useCase.findByStudentId(studentId); }
    @Operation(summary = "Lister les factures d'un établissement — passer academicYearId pour limiter à une année scolaire (recommandé : un appel sans filtre mélange toutes les années créées)")
    @PreAuthorize("hasAuthority('facture:read')")
    @GetMapping("/tenant/{tenantId}") public List<Invoice> findByTenant(@PathVariable Long tenantId, @RequestParam(required = false) Long academicYearId) {
        return academicYearId != null ? useCase.findByTenantIdAndAcademicYearId(tenantId, academicYearId) : useCase.findByTenantId(tenantId);
    }
    @Operation(summary = "Lister les factures d'un établissement filtrées par statut de paiement")
    @PreAuthorize("hasAuthority('facture:read')")
    @GetMapping("/tenant/{tenantId}/status/{status}") public List<Invoice> findByStatus(@PathVariable Long tenantId, @PathVariable InvoiceStatus status) { return useCase.findByTenantIdAndStatus(tenantId, status); }
    @Operation(summary = "Appliquer un montant de paiement à une facture")
    @PreAuthorize("hasAuthority('paiement:create')")
    @PutMapping("/{id}/payment") public ResponseEntity<Invoice> applyPayment(@PathVariable Long id, @RequestBody PaymentRequest r,
            @AuthenticationPrincipal Jwt jwt) {
        // Sans collectedByUserId, CommunicationEventListener#notifyFounder ne peut jamais alerter
        // l'opérateur qui a physiquement encaissé (§retour comptable) — seul le SCHOOL_ADMIN l'était,
        // jamais le comptable connecté ayant utilisé cet endpoint plutôt que /api/v1/payments.
        Long collectedByUserId;
        try { collectedByUserId = jwt == null ? null : Long.valueOf(jwt.getSubject()); }
        catch (NumberFormatException e) { collectedByUserId = null; }
        return ResponseEntity.ok(useCase.applyPayment(id, r.amount(), true, null, collectedByUserId));
    }
    @Operation(summary = "Appliquer une remise / bourse à une facture (recalcule le reste à payer)")
    @PreAuthorize("hasAuthority('facture:update')")
    @PatchMapping("/{id}/discount") public ResponseEntity<Invoice> applyDiscount(@PathVariable Long id, @RequestBody DiscountRequest r) { return ResponseEntity.ok(useCase.applyDiscount(id, r.amount(), r.reason())); }
    @Operation(summary = "Inclure/exclure une facture précise (enfant de personnel) de la retenue automatique "
            + "sur salaire — ex. un frais annexe facultatif (Maillots, T-shirts, Tenue scolaire) que le "
            + "parent préfère régler lui-même (§payroll, modal \"Détail de la retenue\")")
    @PreAuthorize("hasAuthority('paie:manage')")
    @PatchMapping("/{id}/staff-deduction")
    public ResponseEntity<Invoice> setExcludedFromStaffDeduction(@PathVariable Long id, @RequestParam boolean excluded) {
        return ResponseEntity.ok(useCase.setExcludedFromStaffDeduction(id, excluded));
    }
    @Operation(summary = "Statut de retenue sur salaire de cet élève (enfant de personnel ou non, "
            + "actuellement exclu ou non) — pour afficher le bouton retirer/réintégrer sur la fiche élève "
            + "et le modal d'encaissement (§retour comptable)")
    @PreAuthorize("hasAuthority('paie:manage')")
    @GetMapping("/staff-deduction/status")
    public InvoiceUseCase.StaffChildDeductionStatus staffChildDeductionStatus(@RequestParam Long studentId) {
        return useCase.staffChildDeductionStatus(studentId);
    }
    @Operation(summary = "Exclure/réintégrer CET ÉLÈVE de la retenue sur salaire (tous ses parents "
            + "personnel) — depuis la fiche élève ou le modal d'encaissement, pour le cas où certains "
            + "enfants d'un même salarié sont à charge de l'avantage personnel et d'autres payés "
            + "directement par le parent. Rend immédiatement ses factures encore dues (dé)verrouillables "
            + "au guichet (§retour comptable)")
    @PreAuthorize("hasAuthority('paie:manage')")
    @PatchMapping("/staff-deduction/student")
    public void setStaffChildDeductionExcluded(@RequestParam Long studentId, @RequestParam boolean excluded) {
        useCase.setStaffChildDeductionExcluded(studentId, excluded);
    }
    @Operation(summary = "Rattrapage ponctuel : retague \"avantage personnel\" toute facture encore due "
            + "(inscription, réinscription, annexe, mensualité...) d'un enfant de personnel qui n'a "
            + "actuellement aucune remise — pour les factures émises avant que le lien parent↔personnel "
            + "soit établi ou avant un correctif, jamais retouchées depuis (§retour comptable : \"ces "
            + "factures ne se grisent pas pour retenue sur salaire\"). Sûr à relancer plusieurs fois.")
    @PreAuthorize("hasAuthority('paie:manage')")
    @PostMapping("/staff-deduction/backfill")
    public InvoiceUseCase.StaffBenefitBackfillResult backfillStaffBenefitTagging() {
        return useCase.backfillStaffBenefitTagging(gn.lacaeducation.common.TenantContext.get());
    }
    // facture:create (pas facture:update) : ce n'est pas une correction administrative d'une facture
    // existante, mais une étape normale du processus d'encaissement — le comptable l'applique
    // lui-même juste avant de faire payer toutes les tranches restantes en une fois (§billing).
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
    @PreAuthorize("hasAuthority('facture:update')")
    @PatchMapping("/{id}/recovery/advance") public ResponseEntity<Invoice> advanceRecovery(@PathVariable Long id) { return ResponseEntity.ok(useCase.advanceRecovery(id)); }
    @Operation(summary = "Positionner l'étape de recouvrement")
    @PreAuthorize("hasAuthority('facture:update')")
    @PatchMapping("/{id}/recovery") public ResponseEntity<Invoice> setRecovery(@PathVariable Long id, @RequestParam gn.lacaeducation.billing.domain.RecoveryStage stage) { return ResponseEntity.ok(useCase.setRecoveryStage(id, stage)); }
    @Operation(summary = "Annuler une facture")
    @PreAuthorize("hasAuthority('facture:update')")
    @DeleteMapping("/{id}/cancel") public ResponseEntity<Void> cancel(@PathVariable Long id) { useCase.cancel(id); return ResponseEntity.noContent().build(); }
    @Operation(summary = "Supprimer une facture")
    @PreAuthorize("hasAuthority('facture:delete')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
