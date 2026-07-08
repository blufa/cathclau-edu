package gn.lacaeducation.biometric.adapters.in.web;

import gn.lacaeducation.biometric.application.service.BiometricService;
import gn.lacaeducation.biometric.application.service.BiometricService.BadgeResult;
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

    @Operation(summary = "Générer un badge QR pour une personne (auto-enrôlement)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping
    public BadgeResult generate(@RequestBody BadgeRequest r) {
        return service.generateBadge(TenantContext.get(), r.subjectType(), r.subjectId());
    }
}
