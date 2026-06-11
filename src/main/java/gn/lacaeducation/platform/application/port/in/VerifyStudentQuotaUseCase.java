package gn.lacaeducation.platform.application.port.in;

import gn.lacaeducation.platform.TenantQuotaService;

public interface VerifyStudentQuotaUseCase extends TenantQuotaService {
    boolean isQuotaAvailable(Long tenantId, int currentStudentCount);
}
