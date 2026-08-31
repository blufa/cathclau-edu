package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.common.DpeScopeProvider;
import gn.lacaeducation.platform.application.port.out.PrefectureRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Prefecture;
import gn.lacaeducation.platform.domain.PrefectureStatus;
import gn.lacaeducation.platform.domain.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class DpeScopeProviderAdapter implements DpeScopeProvider {

    private final PrefectureRepository prefectureRepository;
    private final TenantRepository tenantRepository;

    @Override
    public Set<Long> tenantIdsForUser(Long userId) {
        var prefectureIds = prefectureRepository.findPrefectureIdsByUserId(userId);
        if (prefectureIds.isEmpty()) {
            return Set.of();
        }
        return tenantRepository.findByPrefectureIdIn(prefectureIds).stream()
                .map(Tenant::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> prefectureIdsForUser(Long userId) {
        return Set.copyOf(prefectureRepository.findPrefectureIdsByUserId(userId));
    }

    @Override
    public Set<Long> tenantIdsForPrefecture(Long prefectureId) {
        return tenantRepository.findByPrefectureIdIn(Set.of(prefectureId)).stream()
                .map(Tenant::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> activePrefectureIds() {
        return prefectureRepository.findAll().stream()
                .filter(p -> p.getStatut() == PrefectureStatus.ACTIVE)
                .map(Prefecture::getId)
                .collect(Collectors.toSet());
    }
}
