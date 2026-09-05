package gn.lacaeducation.courses.domain;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CourseResource {
    private Long id;
    private Long courseId;
    private CourseResourceType type;
    private String title;
    /** Nul tant que l'élève n'a pas payé le cours (voir CoursePurchaseUseCase#accessibleResources). */
    private String fileUrl;
    private Integer sortOrder;
    /** Durée en secondes — uniquement pour les ressources VIDEO. */
    private Integer durationSeconds;
}
