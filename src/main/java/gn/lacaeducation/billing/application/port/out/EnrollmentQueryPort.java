package gn.lacaeducation.billing.application.port.out;

import java.util.List;

/**
 * Port for querying enrollment data from the students module.
 * Implemented by an adapter that reads directly from the enrollments table,
 * avoiding direct coupling to the students module beans.
 */
public interface EnrollmentQueryPort {
    /**
     * Returns the IDs of all students with a VALIDATED enrollment in the given class.
     */
    List<Long> findValidatedStudentIdsByClassId(Long classId);

    /** Whether the student has at least one non-cancelled enrollment (PENDING or VALIDATED). */
    boolean hasActiveEnrollment(Long studentId);

    /** Le parent a-t-il accepté l'accès à la plateforme pour l'inscription de cet élève sur cette
     * année scolaire ? Conditionne l'ajout du frais d'usage plateforme aux mensualités (§billing). */
    boolean hasPlatformAccessOptIn(Long studentId, Long academicYearId);

    /** Ce parent règle-t-il mois par mois plutôt que par tranche groupée pour cette inscription ?
     * Conditionne, à la génération des mensualités, l'éclatement d'une tranche multi-mois en autant
     * de factures séparées d'un mois pour cet élève précisément (§billing). */
    boolean hasMonthlyPaymentOptIn(Long studentId, Long academicYearId);

    /** Année scolaire de l'inscription active (non annulée) la plus récente de cet élève, ou {@code
     * null} s'il n'en a aucune. Filet de sécurité utilisé quand un appelant de facturation omet de
     * préciser l'année scolaire — sans ça, la facture se retrouvait avec `academic_year_id = NULL`,
     * invisible des tableaux de bord qui filtrent strictement par année (ex. Vision Fondateur). */
    Long findActiveAcademicYearId(Long studentId);

    /** Cycle, libellé et sort_order du niveau de l'inscription active la plus récente de l'élève, ou
     * {@code null} s'il n'en a aucune. Utilisé pour déterminer le cadet d'une fratrie par niveau plutôt
     * que par date de naissance (§5, plus fiable : le niveau est toujours renseigné, l'état civil
     * parfois approximatif) — {@code cycle}/{@code label} permettent un classement selon la
     * progression pédagogique officielle (garderie → petite/moyenne/grande section → CP1…CM2 →
     * 7e…10e → 11e…terminale), plus fiable que le seul {@code sortOrder} que chaque école saisit
     * elle-même à la main et peut mal ordonner (ex. grande section avant moyenne section). */
    record ActiveLevel(String cycle, String label, Integer sortOrder) {}

    ActiveLevel findActiveLevel(Long studentId);
}
