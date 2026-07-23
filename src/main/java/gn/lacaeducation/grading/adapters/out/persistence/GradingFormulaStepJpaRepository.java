package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface GradingFormulaStepJpaRepository extends JpaRepository<GradingFormulaStepJpaEntity, Long> {
    List<GradingFormulaStepJpaEntity> findByFormulaIdOrderByStepOrder(Long formulaId);
    void deleteByFormulaId(Long formulaId);
}
