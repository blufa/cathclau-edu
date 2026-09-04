package gn.lacaeducation.staff.adapters.out.persistence;

import gn.lacaeducation.platform.TeacherCountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Implements the platform module's {@link TeacherCountPort}, mirroring StudentCountAdapter's
 * pattern exactly (static dependency staff -> platform, no module cycle). */
@Component
@RequiredArgsConstructor
public class TeacherCountAdapter implements TeacherCountPort {

    private final EnseignantJpaRepository jpaRepository;

    @Override
    public long countByTenant(Long tenantId) {
        return jpaRepository.countByTenantId(tenantId);
    }

    @Override
    public long countActiveByTenant(Long tenantId) {
        return jpaRepository.countByTenantIdAndActiveTrue(tenantId);
    }
}
