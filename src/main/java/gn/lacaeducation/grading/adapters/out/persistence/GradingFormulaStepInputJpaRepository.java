package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface GradingFormulaStepInputJpaRepository extends JpaRepository<GradingFormulaStepInputJpaEntity, Long> {
    List<GradingFormulaStepInputJpaEntity> findByStepId(Long stepId);
    void deleteByStepIdIn(List<Long> stepIds);
}
