package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.application.port.out.GradingScaleRepository;
import gn.lacaeducation.grading.domain.GradingScale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class GradingScalePersistenceAdapter implements GradingScaleRepository {

    private final GradingScaleJpaRepository repo;

    @Override
    public Optional<GradingScale> findByTenantId(Long tenantId) {
        return repo.findByTenantId(tenantId).map(this::toDomain);
    }

    @Override
    public GradingScale save(GradingScale s) {
        GradingScaleJpaEntity e = repo.findByTenantId(s.getTenantId()).orElseGet(GradingScaleJpaEntity::new);
        e.setTenantId(s.getTenantId());
        e.setPassThreshold(s.getPassThreshold());
        e.setPassableMin(s.getPassableMin());
        e.setAssezBienMin(s.getAssezBienMin());
        e.setBienMin(s.getBienMin());
        e.setTresBienMin(s.getTresBienMin());
        e.setFelicitationsMin(s.getFelicitationsMin());
        e.setComplimentsMin(s.getComplimentsMin());
        e.setDistinctionNoFailingThreshold(s.getDistinctionNoFailingThreshold());
        return toDomain(repo.save(e));
    }

    private GradingScale toDomain(GradingScaleJpaEntity e) {
        return GradingScale.builder()
                .id(e.getId()).tenantId(e.getTenantId())
                .passThreshold(e.getPassThreshold()).passableMin(e.getPassableMin())
                .assezBienMin(e.getAssezBienMin()).bienMin(e.getBienMin()).tresBienMin(e.getTresBienMin())
                .felicitationsMin(e.getFelicitationsMin()).complimentsMin(e.getComplimentsMin())
                .distinctionNoFailingThreshold(e.getDistinctionNoFailingThreshold())
                .build();
    }
}
