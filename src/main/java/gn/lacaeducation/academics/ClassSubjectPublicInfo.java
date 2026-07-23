package gn.lacaeducation.academics;

import java.math.BigDecimal;

/**
 * Public view of a class-subject assignment (ClasseMatiere) exposed to other modules:
 * the class, the subject, its coefficient, the assigned teacher and their code (e.g. "CH1") for
 * that subject, if one has been assigned.
 */
public record ClassSubjectPublicInfo(Long classId, Long subjectId, BigDecimal coefficient, Long teacherId, String teacherCode) {}
