package gn.lacaeducation.platform.adapters.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.platform.adapters.in.web.dto.PlatformFeeSettlementResponse;
import gn.lacaeducation.platform.application.port.in.PlatformFeeSettlementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * École (SCHOOL_ADMIN/DIRECTOR/ACCOUNTANT) : solde des frais plateforme dû (accumulé au fil des
 * encaissements de mensualités, voir {@code PlatformFeeLedgerListener}) et son règlement via Djomy —
 * le bouton « Verser » du dashboard finance redirige vers le portail retourné par
 * {@link #initiate}.
 */
@Tag(name = "Platform fee settlement", description = "Solde dû à la plateforme et règlement via Djomy")
@RestController
@RequestMapping("/api/v1/platform-fee")
@RequiredArgsConstructor
public class PlatformFeeSettlementController {

    private final PlatformFeeSettlementUseCase useCase;

    public record BalanceResponse(BigDecimal balance) {}

    public record InitiateRequest(@NotBlank String payerNumber) {}

    @Operation(summary = "Solde actuellement dû à la plateforme par l'école courante")
    @GetMapping("/balance")
    @PreAuthorize("hasAuthority('abonnement:read')")
    public BalanceResponse balance() {
        return new BalanceResponse(useCase.currentBalance());
    }

    @Operation(summary = "Initie le règlement du solde dû (redirection Djomy) — 422 si rien n'est dû")
    @PostMapping("/settlements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    public PlatformFeeSettlementResponse initiate(@RequestBody InitiateRequest req) throws JsonProcessingException {
        return PlatformFeeSettlementResponse.from(useCase.initiate(req.payerNumber()));
    }

    @Operation(summary = "Vérifie le statut Djomy à la demande et confirme si SUCCESS (idempotent)")
    @PostMapping("/settlements/{id}/confirm-djomy")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    public PlatformFeeSettlementResponse confirmViaDjomy(@PathVariable Long id) throws JsonProcessingException {
        return PlatformFeeSettlementResponse.from(useCase.confirmViaDjomy(id));
    }

    @Operation(summary = "Historique des règlements de l'école courante")
    @GetMapping("/settlements")
    @PreAuthorize("hasAuthority('abonnement:read')")
    public List<PlatformFeeSettlementResponse> history() {
        return useCase.findByTenantId(TenantContext.get()).stream()
                .map(PlatformFeeSettlementResponse::from).toList();
    }
}
