package gn.lacaeducation.grading.domain;

import lombok.*;

/**
 * Réglage par établissement (tenant) de la durée des séances d'enseignement générées lors de la
 * création d'une évaluation, par type d'évaluation. Ces séances sont comptabilisées dans les heures
 * travaillées de l'enseignant. {@link #defaults()} reprend les durées standard portées par
 * {@link AssessmentType}.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AssessmentSessionSettings {
    private Long id;
    private Long tenantId;
    private Integer quizMinutes;
    private Integer homeworkMinutes;
    private Integer examMinutes;
    private Integer finalMinutes;

    public static AssessmentSessionSettings defaults() {
        return AssessmentSessionSettings.builder()
                .quizMinutes(AssessmentType.QUIZ.sessionMinutes())
                .homeworkMinutes(AssessmentType.HOMEWORK.sessionMinutes())
                .examMinutes(AssessmentType.EXAM.sessionMinutes())
                .finalMinutes(AssessmentType.FINAL.sessionMinutes())
                .build();
    }

    /** Durée configurée pour ce type, ou la durée par défaut du type si non renseignée. */
    public int minutesFor(AssessmentType type) {
        if (type == null) return AssessmentType.HOMEWORK.sessionMinutes();
        Integer configured = switch (type) {
            case QUIZ -> quizMinutes;
            case HOMEWORK -> homeworkMinutes;
            case EXAM -> examMinutes;
            case FINAL -> finalMinutes;
        };
        return configured != null && configured > 0 ? configured : type.sessionMinutes();
    }
}
