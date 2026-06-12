package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.grading.application.port.in.GradingScaleUseCase;
import gn.lacaeducation.grading.application.port.out.GradingScaleRepository;
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
    public GradingScale getForTenant(Long tenantId) {
        GradingScale defaults = GradingScale.defaults();
        defaults.setTenantId(tenantId);
        return repo.findByTenantId(tenantId).orElse(defaults);
    }

    @Override
    public GradingScale update(Long tenantId, UpdateScaleCommand c) {
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
        return repo.save(GradingScale.builder()
                .tenantId(tenantId)
                .passThreshold(c.passThreshold())
                .passableMin(c.passableMin())
                .assezBienMin(c.assezBienMin())
                .bienMin(c.bienMin())
                .tresBienMin(c.tresBienMin())
                .build());
    }
}
