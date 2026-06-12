package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.GradingTeacherProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Exposes to other modules whether a teacher owns any evaluation in grading. */
@Component
@RequiredArgsConstructor
public class GradingTeacherProviderAdapter implements GradingTeacherProvider {

    private final AssessmentJpaRepository assessmentRepo;

    @Override
    public boolean teacherHasEvaluations(Long teacherId) {
        return assessmentRepo.existsByTeacherId(teacherId);
    }
}
