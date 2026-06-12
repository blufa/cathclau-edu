package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.platform.StudentCountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Provides the platform module with the student count per tenant, implementing
 * the platform outbound port. The static dependency direction is students -> platform,
 * matching the existing TenantQuotaService usage, so no module cycle is introduced.
 */
@Component
@RequiredArgsConstructor
public class StudentCountAdapter implements StudentCountPort {

    private final EleveJpaRepository eleveRepo;

    @Override
    public long countByTenant(Long tenantId) {
        return eleveRepo.countByTenantId(tenantId);
    }
}
