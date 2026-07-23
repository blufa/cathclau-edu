package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.GradingFormula;

import java.util.List;
import java.util.Optional;

public interface GradingFormulaRepository {
    /** Enregistre la formule et remplace intégralement ses étapes (et leurs entrées) par celles fournies. */
    GradingFormula save(GradingFormula formula);
    Optional<GradingFormula> findById(Long id);
    List<GradingFormula> findByTenantId(Long tenantId);
    /** Formule active du tenant pour un type de période (ex. « SEMESTRE ») ; résolue par le moteur de calcul. */
    Optional<GradingFormula> findActiveByTenantIdAndPeriodType(Long tenantId, String periodType);
    void deleteById(Long id);
}
