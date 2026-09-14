package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.grading.application.port.in.GradingScaleUseCase;
import gn.lacaeducation.grading.application.port.out.GradingScaleRepository;
import gn.lacaeducation.grading.domain.CycleGroup;
import gn.lacaeducation.grading.domain.GradingScale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
public class GradingScaleService implements GradingScaleUseCase {

    private final GradingScaleRepository repo;

    @Override
    @Transactional(readOnly = true)
    public GradingScale getForTenant(Long tenantId, CycleGroup cycleGroup) {
        GradingScale defaults = GradingScale.defaults(cycleGroup);
        defaults.setTenantId(tenantId);
        return repo.findByTenantIdAndCycleGroup(tenantId, cycleGroup).orElse(defaults);
    }

    @Override
    public GradingScale update(Long tenantId, CycleGroup cycleGroup, UpdateScaleCommand c) {
        // Bands must be strictly increasing and within [0, 20].
        BigDecimal[] bands = {c.passableMin(), c.assezBienMin(), c.bienMin(), c.tresBienMin()};
        for (int i = 1; i < bands.length; i++) {
            if (bands[i] == null || bands[i - 1] == null || bands[i].compareTo(bands[i - 1]) <= 0) {
                throw new BusinessRuleException("Honor band thresholds must be strictly increasing.");
            }
        }
        if (c.passThreshold() == null || c.passThreshold().compareTo(BigDecimal.ZERO) < 0
                || c.tresBienMin().compareTo(BigDecimal.valueOf(20)) > 0) {
            throw new BusinessRuleException("Thresholds must be within [0, 20].");
        }
        // Distinction thresholds: optional (fall back to defaults), Compliments <= Félicitations.
        GradingScale defaults = GradingScale.defaults();
        BigDecimal felicitations = c.felicitationsMin() != null ? c.felicitationsMin() : defaults.getFelicitationsMin();
        BigDecimal compliments = c.complimentsMin() != null ? c.complimentsMin() : defaults.getComplimentsMin();
        BigDecimal noFailing = c.distinctionNoFailingThreshold() != null
                ? c.distinctionNoFailingThreshold() : defaults.getDistinctionNoFailingThreshold();
        if (compliments.compareTo(felicitations) > 0) {
            throw new BusinessRuleException("Compliments threshold must not exceed the Félicitations threshold.");
        }
        return repo.save(GradingScale.builder()
                .tenantId(tenantId)
                .cycleGroup(cycleGroup)
                .passThreshold(c.passThreshold())
                .passableMin(c.passableMin())
                .assezBienMin(c.assezBienMin())
                .bienMin(c.bienMin())
                .tresBienMin(c.tresBienMin())
                .felicitationsMin(felicitations)
                .complimentsMin(compliments)
                .distinctionNoFailingThreshold(noFailing)
                .build());
    }
}
