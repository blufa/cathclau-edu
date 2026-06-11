package gn.lacaeducation.platform;

public interface TenantQuotaService {
    boolean isQuotaAvailable(Long tenantId, int currentStudentCount);
}
