package gn.lacaeducation.audit.adapters.in.web;

import gn.lacaeducation.audit.adapters.in.web.dto.AuditLogResponse;
import gn.lacaeducation.audit.application.port.in.AuditLogUseCase;
import gn.lacaeducation.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Journal d'audit (§13/§16 cahier des charges) — append-only, aucun endpoint de modification ni de
 * suppression. Vue école ({@code /audit-logs}) scopée au tenant courant ; vue plateforme
 * ({@code /platform/audit-logs}) réservée à SUPER_ADMIN.
 */
@Tag(name = "Audit", description = "Journal d'audit append-only")
@RestController
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogUseCase useCase;

    @Operation(summary = "Journal d'audit de l'école courante")
    @GetMapping("/api/v1/audit-logs")
    @PreAuthorize("hasAuthority('audit:read')")
    public PageResponse<AuditLogResponse> forCurrentTenant(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(useCase.findForCurrentTenant(pageable).map(AuditLogResponse::from));
    }

    @Operation(summary = "Journal d'audit plateforme (actions hors tenant : création/suspension d'école…)")
    @GetMapping("/api/v1/platform/audit-logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PageResponse<AuditLogResponse> platformActions(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(useCase.findPlatformActions(pageable).map(AuditLogResponse::from));
    }

    @Operation(summary = "Journal d'audit complet, toutes écoles confondues (SUPER_ADMIN)")
    @GetMapping("/api/v1/platform/audit-logs/toutes-ecoles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PageResponse<AuditLogResponse> all(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(useCase.findAll(pageable).map(AuditLogResponse::from));
    }
}
