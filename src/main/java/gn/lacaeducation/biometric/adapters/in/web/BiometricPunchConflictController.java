package gn.lacaeducation.biometric.adapters.in.web;

import gn.lacaeducation.biometric.application.service.BiometricService;
import gn.lacaeducation.biometric.domain.BiometricPunchConflict;
import gn.lacaeducation.biometric.domain.ConflictStatus;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Biometric Punch Conflicts", description = "Conflits de pointage détectés (même sujet, appareils incompatibles) — revue humaine obligatoire")
@RestController
@RequestMapping("/api/v1/biometric/punch-conflicts")
@PreAuthorize("hasAuthority('biometrique:manage')")
@RequiredArgsConstructor
public class BiometricPunchConflictController {

    private final BiometricService service;

    @Operation(summary = "Liste des conflits détectés (filtre optionnel par statut)")
    @GetMapping
    public List<BiometricPunchConflict> list(@RequestParam(required = false) ConflictStatus status) {
        return service.conflicts(TenantContext.get(), status);
    }

    public record ReviewRequest(ConflictStatus decision, String note) {}

    @Operation(summary = "Décision humaine sur un conflit (DISMISSED ou CONFIRMED_ANOMALY) — ne modifie jamais les pointages d'origine")
    @PatchMapping("/{id}/review")
    public BiometricPunchConflict review(@PathVariable Long id, @RequestBody ReviewRequest r, @AuthenticationPrincipal Jwt jwt) {
        return service.reviewConflict(id, r.decision(), currentUserId(jwt), r.note());
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
