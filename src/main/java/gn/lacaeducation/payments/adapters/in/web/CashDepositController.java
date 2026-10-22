package gn.lacaeducation.payments.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payments.application.port.in.CashDepositUseCase;
import gn.lacaeducation.payments.domain.CashDeposit;
import gn.lacaeducation.payments.domain.CashDepositStatus;
import gn.lacaeducation.payments.domain.PaymentChannel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Dépôts de fonds comptable → fondateur, avec validation explicite et SMS bidirectionnel (§5 cahier
 * des charges) — voir {@code CashDepositService} pour le détail du workflow. */
@Tag(name = "Cash Deposits", description = "Dépôts comptable → fondateur, validation et historique")
@RestController
@RequestMapping("/api/v1/payments/cash-deposits")
@RequiredArgsConstructor
public class CashDepositController {

    private final CashDepositUseCase useCase;

    public record CreateRequest(BigDecimal amount, String currency, PaymentChannel method,
                                 String destinationAccount, LocalDateTime depositedAt,
                                 String transactionReference, String comment, String idempotencyKey) {}
    public record RejectRequest(String reason) {}

    @Operation(summary = "Enregistrer un dépôt remis au fondateur (en attente de validation) — envoie un SMS au fondateur")
    @PreAuthorize("hasAuthority('depot:create')")
    @PostMapping
    public CashDeposit create(@RequestBody CreateRequest r, @AuthenticationPrincipal Jwt jwt) {
        return useCase.create(new CashDepositUseCase.CreateCashDepositCommand(
                TenantContext.get(), currentUserId(jwt), r.amount(), r.currency(), r.method(),
                r.destinationAccount(), r.depositedAt(), r.transactionReference(), r.comment(), r.idempotencyKey()));
    }

    @Operation(summary = "Détail d'un dépôt")
    @PreAuthorize("hasAuthority('depot:read')")
    @GetMapping("/{id}")
    public CashDeposit get(@PathVariable Long id) { return useCase.findById(id); }

    @Operation(summary = "Historique des dépôts — filtrable par comptable, statut, moyen et période")
    @PreAuthorize("hasAuthority('depot:read')")
    @GetMapping
    public List<CashDeposit> search(@RequestParam(required = false) Long accountantId,
                                     @RequestParam(required = false) CashDepositStatus status,
                                     @RequestParam(required = false) PaymentChannel method,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return useCase.search(TenantContext.get(), accountantId, status, method, from, to);
    }

    @Operation(summary = "Valider un dépôt en attente — envoie un SMS de confirmation au comptable")
    @PreAuthorize("hasAuthority('depot:valider')")
    @PatchMapping("/{id}/validate")
    public CashDeposit validate(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return useCase.validate(id, currentUserId(jwt));
    }

    @Operation(summary = "Rejeter un dépôt en attente (avec motif) — envoie un SMS au comptable")
    @PreAuthorize("hasAuthority('depot:valider')")
    @PatchMapping("/{id}/reject")
    public CashDeposit reject(@PathVariable Long id, @RequestBody RejectRequest r, @AuthenticationPrincipal Jwt jwt) {
        return useCase.reject(id, currentUserId(jwt), r.reason());
    }

    private Long currentUserId(Jwt jwt) {
        try { return jwt == null ? null : Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
