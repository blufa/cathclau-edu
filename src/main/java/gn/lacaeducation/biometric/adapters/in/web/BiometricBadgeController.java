package gn.lacaeducation.biometric.adapters.in.web;

import gn.lacaeducation.biometric.application.service.BiometricService;
import gn.lacaeducation.biometric.application.service.BiometricService.BadgeResult;
import gn.lacaeducation.biometric.application.service.BiometricService.ResolvedBadge;
import gn.lacaeducation.biometric.domain.BiometricEnrollment;
import gn.lacaeducation.biometric.domain.BiometricSubjectType;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Biometric Badges", description = "Génération de badge QR — enrôle automatiquement la personne pour le pointage")
@RestController
@RequestMapping("/api/v1/biometric/badges")
@RequiredArgsConstructor
public class BiometricBadgeController {

    private final BiometricService service;

    public record BadgeRequest(BiometricSubjectType subjectType, Long subjectId) {}

    @Operation(summary = "Générer un badge QR pour une personne (auto-enrôlement). Révoque silencieusement tout ancien QR déjà imprimé pour elle.")
    @PreAuthorize("hasAuthority('biometrique:manage')")
    @PostMapping
    public BadgeResult generate(@RequestBody BadgeRequest r) {
        return service.generateBadge(TenantContext.get(), r.subjectType(), r.subjectId());
    }

    @Operation(summary = "Résout un jeton de badge scanné (QR « BADGE2|<jeton> ») en la personne correspondante, pour le pointage")
    @PreAuthorize("hasAuthority('presence:create') or hasRole('STUDENT')")
    @GetMapping("/resolve")
    public ResolvedBadge resolve(@RequestParam String token) {
        return service.resolveBadge(token);
    }

    public record NfcUidRequest(String nfcUid) {}

    @Operation(summary = "Associe (ou retire, si vide) l'UID d'une carte NFC physique à un badge déjà généré")
    @PreAuthorize("hasAuthority('biometrique:manage')")
    @PatchMapping("/{enrollmentId}/nfc")
    public BiometricEnrollment associateNfc(@PathVariable Long enrollmentId, @RequestBody NfcUidRequest r) {
        return service.associateNfcUid(enrollmentId, r.nfcUid());
    }

    @Operation(summary = "Résout un UID de carte NFC lu en la personne correspondante, pour le pointage")
    @PreAuthorize("hasAuthority('presence:create') or hasRole('STUDENT')")
    @GetMapping("/resolve-nfc")
    public ResolvedBadge resolveNfc(@RequestParam String uid) {
        return service.resolveBadgeByNfc(uid);
    }
}
