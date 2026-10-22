package gn.lacaeducation.billing.adapters.in.web;

import gn.lacaeducation.billing.application.port.in.StudentCreditUseCase;
import gn.lacaeducation.billing.domain.StudentCredit;
import gn.lacaeducation.billing.domain.StudentCreditUsage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trop-perçu conservé comme crédit disponible de l'élève (§4 cahier des charges) — posé
 * automatiquement par un encaissement supérieur au solde dû d'une facture, applicable ensuite à
 * n'importe quelle facture future du même élève.
 */
@Tag(name = "Student credits", description = "Crédit disponible d'un élève (trop-perçu)")
@RestController
@RequestMapping("/api/v1/billing/student-credits")
@RequiredArgsConstructor
public class StudentCreditController {

    private final StudentCreditUseCase useCase;

    public record BalanceResponse(BigDecimal availableBalance) {}
    public record ApplyRequest(Long invoiceId, BigDecimal amount) {}
    public record ApplyResponse(BigDecimal appliedAmount) {}

    @Operation(summary = "Solde de crédit disponible (trop-perçu) de cet élève")
    @PreAuthorize("hasAuthority('facture:read')")
    @GetMapping("/student/{studentId}/balance")
    public BalanceResponse balance(@PathVariable Long studentId) {
        return new BalanceResponse(useCase.availableBalance(studentId));
    }

    @Operation(summary = "Détail des lots de crédit de cet élève (épuisés inclus)")
    @PreAuthorize("hasAuthority('facture:read')")
    @GetMapping("/student/{studentId}")
    public List<StudentCredit> findByStudent(@PathVariable Long studentId) {
        return useCase.findByStudentId(studentId);
    }

    @Operation(summary = "Historique des applications de crédit de cet élève")
    @PreAuthorize("hasAuthority('facture:read')")
    @GetMapping("/student/{studentId}/usages")
    public List<StudentCreditUsage> usageHistory(@PathVariable Long studentId) {
        return useCase.findUsageHistory(studentId);
    }

    @Operation(summary = "Applique le crédit disponible de l'élève à une de ses factures")
    @PreAuthorize("hasAuthority('paiement:create')")
    @PostMapping("/student/{studentId}/apply")
    public ApplyResponse apply(@PathVariable Long studentId, @RequestBody ApplyRequest r,
                               @AuthenticationPrincipal Jwt jwt) {
        BigDecimal applied = useCase.applyToInvoice(studentId, r.invoiceId(), r.amount(), currentUserId(jwt));
        return new ApplyResponse(applied);
    }

    private static Long currentUserId(Jwt jwt) {
        try {
            return jwt == null ? null : Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
