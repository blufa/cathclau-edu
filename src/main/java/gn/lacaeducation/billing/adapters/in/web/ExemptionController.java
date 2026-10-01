package gn.lacaeducation.billing.adapters.in.web;

import gn.lacaeducation.billing.application.port.in.ExemptionUseCase;
import gn.lacaeducation.billing.application.port.in.ExemptionUseCase.GrantExemptionCommand;
import gn.lacaeducation.billing.domain.Exemption;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Exonérations / remises exceptionnelles (§8). Accord réservé à la direction (traçabilité du
 * responsable). Jamais supprimées — seulement révoquées (archivage).
 */
@Tag(name = "Exemptions", description = "Exonérations et remises exceptionnelles (§8)")
@RestController
@RequestMapping("/api/v1/billing/exemptions")
@RequiredArgsConstructor
public class ExemptionController {

    private final ExemptionUseCase useCase;

    @Operation(summary = "Accorder une exonération à un élève (direction uniquement)")
    @PreAuthorize("hasAuthority('regle_facturation:create')")
    @PostMapping
    public ResponseEntity<Exemption> grant(@RequestBody GrantExemptionCommand cmd, @AuthenticationPrincipal Jwt jwt) {
        Exemption e = useCase.grant(TenantContext.get(), cmd, currentUserId(jwt));
        return ResponseEntity.created(URI.create("/api/v1/billing/exemptions/" + e.getId())).body(e);
    }

    // Lecture ouverte aussi au comptable : regle_facturation:read, accordé en lecture seule — sert par
    // ex. au badge "Exonéré" de la liste des élèves (§students), que le comptable doit voir sans avoir
    // la main sur l'octroi/la révocation (regle_facturation:create/delete ci-dessous).
    @Operation(summary = "Lister les exonérations du tenant (archive complète)")
    @PreAuthorize("hasAuthority('regle_facturation:read')")
    @GetMapping
    public List<Exemption> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Historique des exonérations d'un élève")
    @PreAuthorize("hasAuthority('regle_facturation:read')")
    @GetMapping("/student/{studentId}")
    public List<Exemption> byStudent(@PathVariable Long studentId) {
        return useCase.findByStudent(studentId);
    }

    @Operation(summary = "Révoquer une exonération (archivée, jamais supprimée)")
    @PreAuthorize("hasAuthority('regle_facturation:delete')")
    @PatchMapping("/{id}/revoquer")
    public Exemption revoke(@PathVariable Long id) {
        return useCase.revoke(id);
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
