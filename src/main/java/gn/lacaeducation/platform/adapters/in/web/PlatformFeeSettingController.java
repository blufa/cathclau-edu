package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.application.port.in.PlatformFeeSettingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * SUPER_ADMIN : montant du frais d'usage de la plateforme, ajouté aux factures de mensualité de
 * toutes les écoles (voir {@code PlatformFeeSettlementController} côté école pour le règlement).
 */
@Tag(name = "Platform fee", description = "Frais d'usage de la plateforme, appliqué aux mensualités des écoles")
@RestController
@RequestMapping("/api/v1/platform/frais-plateforme")
@RequiredArgsConstructor
public class PlatformFeeSettingController {

    private final PlatformFeeSettingUseCase useCase;

    public record PlatformFeeSettingResponse(BigDecimal amount) {}

    public record SetAmountRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero
                                    BigDecimal amount) {}

    @Operation(summary = "Montant actuellement configuré (zéro si jamais défini)")
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlatformFeeSettingResponse current() {
        return new PlatformFeeSettingResponse(useCase.currentAmount());
    }

    @Operation(summary = "Définit le nouveau montant : s'applique aux factures générées à partir de "
            + "maintenant, jamais rétroactivement aux factures déjà émises")
    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlatformFeeSettingResponse setAmount(@Valid @RequestBody SetAmountRequest req,
                                                 @AuthenticationPrincipal Jwt jwt) {
        var setting = useCase.setAmount(req.amount(), userIdOf(jwt));
        return new PlatformFeeSettingResponse(setting.getAmount());
    }

    private static Long userIdOf(Jwt jwt) {
        try { return jwt == null ? null : Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
