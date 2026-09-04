package gn.lacaeducation.audit.adapters.in.web.dto;

import gn.lacaeducation.audit.domain.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long tenantId,
        Long utilisateurId,
        String action,
        String ressource,
        Long ressourceId,
        String details,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getTenantId(), log.getUtilisateurId(),
                log.getAction(), log.getRessource(), log.getRessourceId(), log.getDetails(), log.getCreatedAt());
    }
}
