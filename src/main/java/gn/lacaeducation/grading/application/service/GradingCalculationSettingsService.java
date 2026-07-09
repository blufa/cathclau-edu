package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.grading.application.port.in.GradingCalculationSettingsUseCase;
import gn.lacaeducation.grading.application.port.out.GradingCalculationSettingsRepository;
import gn.lacaeducation.grading.domain.GradingCalculationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class GradingCalculationSettingsService implements GradingCalculationSettingsUseCase {

    private static final int MAX_DECIMALS = 3;

    private final GradingCalculationSettingsRepository repo;

    @Override
    @Transactional(readOnly = true)
    public GradingCalculationSettings getForTenant(Long tenantId) {
        GradingCalculationSettings defaults = GradingCalculationSettings.defaults();
        defaults.setTenantId(tenantId);
        return repo.findByTenantId(tenantId).orElse(defaults);
    }

    @Override
    public GradingCalculationSettings update(Long tenantId, UpdateCommand c) {
        if (c.decimals() != null && (c.decimals() < 0 || c.decimals() > MAX_DECIMALS)) {
            throw new BusinessRuleException("Le nombre de décimales doit être compris entre 0 et " + MAX_DECIMALS + ".");
        }
        GradingCalculationSettings defaults = GradingCalculationSettings.defaults();
        return repo.save(GradingCalculationSettings.builder()
                .tenantId(tenantId)
                .averageMethod(c.averageMethod() != null ? c.averageMethod() : defaults.getAverageMethod())
                .decimals(c.decimals() != null ? c.decimals() : defaults.getDecimals())
                .rounding(c.rounding() != null ? c.rounding() : defaults.getRounding())
                .build());
    }
}
