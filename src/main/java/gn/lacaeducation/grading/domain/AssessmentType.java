package gn.lacaeducation.grading.domain;

/**
 * Type d'évaluation. Chaque type porte la durée par défaut de la séance de travail correspondante
 * (comptabilisée dans les heures de l'enseignant) : une interrogation est plus courte qu'une
 * composition ou un examen final.
 */
public enum AssessmentType {
    QUIZ(30),      // interrogation
    HOMEWORK(60),  // devoir
    EXAM(120),     // composition / devoir surveillé
    FINAL(180);    // examen de fin de période

    private final int sessionMinutes;

    AssessmentType(int sessionMinutes) {
        this.sessionMinutes = sessionMinutes;
    }

    /** Durée par défaut (minutes) de la séance d'enseignement générée pour ce type d'évaluation. */
    public int sessionMinutes() {
        return sessionMinutes;
    }
}
