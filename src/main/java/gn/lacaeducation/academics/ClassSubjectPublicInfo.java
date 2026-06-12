package gn.lacaeducation.academics;

import java.math.BigDecimal;

/**
 * Public view of a class-subject assignment (ClasseMatiere) exposed to other modules:
 * the subject, its coefficient and the assigned teacher for a given class.
 */
public record ClassSubjectPublicInfo(Long subjectId, BigDecimal coefficient, Long teacherId) {}
