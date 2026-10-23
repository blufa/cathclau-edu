package gn.lacaeducation.grading.domain;

/**
 * Type d'évaluation. Chaque type porte la durée par défaut de la séance de travail correspondante
 * (comptabilisée dans les heures de l'enseignant) : une interrogation est plus courte qu'une
 * composition ou un examen final.
 */
public enum AssessmentType {
    QUIZ(30, "Interrogation"),
    HOMEWORK(60, "Devoir"),
    EXAM(120, "Composition"),
    FINAL(180, "Examen");

    private final int sessionMinutes;
    private final String frenchLabel;

    AssessmentType(int sessionMinutes, String frenchLabel) {
        this.sessionMinutes = sessionMinutes;
        this.frenchLabel = frenchLabel;
    }

    /** Durée par défaut (minutes) de la séance d'enseignement générée pour ce type d'évaluation. */
    public int sessionMinutes() {
        return sessionMinutes;
    }

    /** Libellé affichable (jamais le nom brut de l'enum, qui n'a de sens qu'en interne) — miroir de
     * {@code ASSESSMENT_TYPE_LABEL} côté frontend (lacaeducation-ui, src/lib/types/grading.ts). */
    public String frenchLabel() {
        return frenchLabel;
    }
}
