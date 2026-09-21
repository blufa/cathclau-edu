package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.application.port.in.PlatformFeeSettlementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SUPER_ADMIN : combien chaque école doit actuellement à la plateforme au titre du frais d'usage
 * (désormais un frais annexe configuré librement par chaque école, voir FeeType.PLATFORM — le montant
 * n'est plus fixé par SUPER_ADMIN, seul ce tableau de suivi des soldes reste plateforme-wide).
 */
@Tag(name = "Platform fee", description = "Solde dû par chaque école au titre du frais d'usage de la plateforme")
@RestController
@RequestMapping("/api/v1/platform/frais-plateforme")
@RequiredArgsConstructor
public class PlatformFeeBalanceController {

    private final PlatformFeeSettlementUseCase settlementUseCase;

    @Operation(summary = "Solde dû par chaque école (toutes, y compris celles à 0) — combien chaque "
            + "école doit actuellement à la plateforme")
    @GetMapping("/soldes")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<PlatformFeeSettlementUseCase.TenantBalance> balances() {
        return settlementUseCase.balancesByTenant();
    }
}
