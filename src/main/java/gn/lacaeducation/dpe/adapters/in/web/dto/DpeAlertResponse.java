package gn.lacaeducation.dpe.adapters.in.web.dto;

import gn.lacaeducation.dpe.domain.DpeAlert;
import gn.lacaeducation.dpe.domain.DpeAlertPriority;
import gn.lacaeducation.dpe.domain.DpeAlertStatus;
import gn.lacaeducation.dpe.domain.DpeAlertType;

import java.time.LocalDateTime;

public record DpeAlertResponse(
        Long id,
        Long tenantId,
        DpeAlertType type,
        DpeAlertPriority priorite,
        String description,
        DpeAlertStatus statut,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {
    public static DpeAlertResponse from(DpeAlert a) {
        return new DpeAlertResponse(a.getId(), a.getTenantId(), a.getType(), a.getPriorite(),
                a.getDescription(), a.getStatut(), a.getCreatedAt(), a.getUpdatedAt(), a.getResolvedAt());
    }
}
