package gn.lacaeducation.grading.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/**
 * Port to query subject-to-class assignments (ClasseMatiere) from the academics module.
 * Provides read-only access to subject coefficients per class.
 */
public interface ClassSubjectQueryPort {

    record ClassSubjectInfo(Long subjectId, BigDecimal coefficient, Long teacherId) {}

    /**
     * Returns all subject assignments (with coefficient and teacher) for a given class.
     */
    List<ClassSubjectInfo> findByClassId(Long classId);
}
