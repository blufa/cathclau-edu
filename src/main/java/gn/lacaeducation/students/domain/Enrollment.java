package gn.lacaeducation.students.domain;

import gn.lacaeducation.students.EnrollmentType;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Enrollment {
    private Long id;
    private Long studentId;
    private Long classId;
    private Long academicYearId;
    private EnrollmentType type;
    private EnrollmentStatus status;
    private LocalDate enrolledAt;
    /** Le parent a accepté que l'élève soit connecté à la plateforme (choix fait à l'inscription,
     * modifiable ensuite) : si oui, le frais d'usage plateforme est ajouté aux mensualités de cette
     * année ; sinon jamais. Défaut : false (opt-in explicite requis). */
    private Boolean platformAccessOptIn;
}
