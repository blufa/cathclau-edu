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
}
