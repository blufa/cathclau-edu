package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter that reads class-subject assignments (with coefficients) from the academics module
 * through its public {@link AcademicsDataProvider} API, keeping the dependency direction
 * grading -> academics without reaching into academics' persistence internals.
 */
@Component
@RequiredArgsConstructor
class ClassSubjectQueryAdapter implements ClassSubjectQueryPort {

    private final AcademicsDataProvider academicsDataProvider;

    @Override
    public List<ClassSubjectInfo> findByClassId(Long classId) {
        return academicsDataProvider.findClassSubjects(classId).stream()
                .map(cs -> new ClassSubjectInfo(cs.subjectId(), cs.coefficient(), cs.teacherId()))
                .toList();
    }
}
