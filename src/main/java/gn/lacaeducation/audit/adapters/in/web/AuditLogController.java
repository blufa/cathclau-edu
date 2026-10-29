package gn.lacaeducation.audit.adapters.in.web;

import gn.lacaeducation.audit.adapters.in.web.dto.AuditLogResponse;
import gn.lacaeducation.audit.adapters.out.AuditResourceNameResolver;
import gn.lacaeducation.audit.application.port.in.AuditLogUseCase;
import gn.lacaeducation.common.PageResponse;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.iam.UserPublicInfoProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

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
    private final AuditResourceNameResolver nameResolver;
    private final UserPublicInfoProvider userProvider;

    @Operation(summary = "Journal d'audit de l'école courante, filtrable par action/utilisateur/période")
    @GetMapping("/api/v1/audit-logs")
    @PreAuthorize("hasAuthority('audit:read')")
    public PageResponse<AuditLogResponse> forCurrentTenant(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        boolean noFilter = action == null && utilisateurId == null && from == null && to == null;
        Page<AuditLogResponse> page = noFilter
                ? useCase.findForCurrentTenant(pageable).map(nameResolver::toResponse)
                : useCase.searchForCurrentTenant(action, utilisateurId, from, to, pageable).map(nameResolver::toResponse);
        return PageResponse.of(page);
    }

    @Operation(summary = "Utilisateurs de l'école courante, pour le filtre \"par utilisateur\" du journal "
            + "d'audit — n'exige que la permission de lecture du journal, pas la gestion complète des "
            + "comptes (certains rôles, ex. comptable, ont l'une sans l'autre)")
    @GetMapping("/api/v1/audit-logs/actors")
    @PreAuthorize("hasAuthority('audit:read')")
    public List<UserPublicInfoProvider.UserPublicInfo> actors() {
        return userProvider.findUsersByTenant(TenantContext.get());
    }

    @Operation(summary = "Journal d'audit plateforme (actions hors tenant : création/suspension d'école…)")
    @GetMapping("/api/v1/platform/audit-logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PageResponse<AuditLogResponse> platformActions(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(useCase.findPlatformActions(pageable).map(nameResolver::toResponse));
    }

    @Operation(summary = "Journal d'audit complet, toutes écoles confondues (SUPER_ADMIN)")
    @GetMapping("/api/v1/platform/audit-logs/toutes-ecoles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PageResponse<AuditLogResponse> all(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(useCase.findAll(pageable).map(nameResolver::toResponse));
    }
}
