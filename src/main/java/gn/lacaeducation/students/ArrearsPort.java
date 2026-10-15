package gn.lacaeducation.students;

import java.math.BigDecimal;

/**
 * Outbound port (part of the students public API) letting students check a student's unpaid balance
 * from a PRIOR academic year (owed to the school) before allowing a re-enrollment, even if the student
 * is academically admitted. The billing module provides the adapter — same cycle-avoidance rationale as
 * {@link ClassCapacityPort}: billing already depends on students (enrollment events), so the only
 * static dependency stays billing -> students.
 */
public interface ArrearsPort {

    record ArrearsInfo(BigDecimal totalDue, int invoiceCount) {}

    /** Somme des soldes restants (UNPAID/PARTIAL) de l'élève, hors l'année {@code excludingAcademicYearId}
     * (celle visée par la réinscription) — jamais null, {@code totalDue} = 0 si aucune dette. */
    ArrearsInfo arrears(Long studentId, Long excludingAcademicYearId);
}
