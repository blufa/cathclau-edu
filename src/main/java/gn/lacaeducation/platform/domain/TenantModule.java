package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Effective module of a tenant (features/SCHOOL_back_complements.md §M): derived from its plan at
 * provisioning, with optional per-tenant override (temporary access via expiration date).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantModule {
    private Long id;
    private Long tenantId;
    private ModuleCode moduleCode;
    private boolean active;
    private LocalDate activationDate;
    private LocalDate expirationDate;

    /** Active and not past its (optional) expiration date. */
    public boolean isEffective(LocalDate today) {
        return active && (expirationDate == null || !expirationDate.isBefore(today));
    }
}
