package gn.lacaeducation.platform.adapters.out.persistence;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.platform.domain.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component @RequiredArgsConstructor
public class TenantInfoProviderAdapter implements TenantInfoProvider {
    private final TenantJpaRepository jpa;
    @Override
    public Optional<TenantPublicInfo> findTenantInfo(Long tenantId) {
        return jpa.findById(tenantId).map(t ->
                new TenantPublicInfo(t.getId(), t.getSchoolName(), t.getEmail(), t.getPhone(),
                        t.getLogo(), t.getAddress(), t.getCity(), t.getCountry(),
                        t.getWebsite(), t.getDirector(),
                        t.getAccreditationNumber(), t.getOpeningAuthorization(),
                        t.getBrandColor(),
                        t.getIre(), t.getDpeLabel(), t.getSousPrefectureLabel(), t.getMotto(),
                        t.getMatriculeFormat(), t.getMatriculeDigits(), t.getFounderPhone(),
                        t.isNotifyStaffOnPayment()));
    }
    @Override
    public boolean isTenantActive(Long tenantId) {
        if (tenantId == null) return true;
        return jpa.findById(tenantId)
                .map(t -> t.getStatus() == TenantStatus.ACTIVE || t.getStatus() == TenantStatus.TRIAL)
                .orElse(false);
    }
}
