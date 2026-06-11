package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.academics.adapters.out.persistence.ClasseMatiereJpaEntity;
import gn.lacaeducation.academics.adapters.out.persistence.MatiereJpaEntity;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter that reads class-subject assignments and subject coefficients directly from the
 * academics module's tables (read-only cross-module DB access via a dedicated port).
 */
@Component
@RequiredArgsConstructor
class ClassSubjectQueryAdapter implements ClassSubjectQueryPort {

    private final ClassSubjectJpaRepository classSubjectJpa;
    private final SubjectCoeffJpaRepository subjectCoeffJpa;

    @Override
    public List<ClassSubjectInfo> findByClassId(Long classId) {
        List<ClasseMatiereJpaEntity> assignments = classSubjectJpa.findBySchoolClassId(classId);
        if (assignments.isEmpty()) {
            return List.of();
        }

        Set<Long> subjectIds = assignments.stream()
                .map(ClasseMatiereJpaEntity::getSubjectId)
                .collect(Collectors.toSet());

        // Fetch all relevant subjects to get coefficients
        Map<Long, BigDecimal> coeffBySubject = subjectCoeffJpa.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(
                        MatiereJpaEntity::getId,
                        s -> s.getCoefficient() != null ? s.getCoefficient() : BigDecimal.ONE
                ));

        return assignments.stream()
                .map(a -> new ClassSubjectInfo(
                        a.getSubjectId(),
                        coeffBySubject.getOrDefault(a.getSubjectId(), BigDecimal.ONE),
                        a.getTeacherId()
                ))
                .toList();
    }
}
