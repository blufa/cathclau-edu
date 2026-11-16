package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.domain.DepartureReason;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import gn.lacaeducation.students.EnrollmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity @Table(name = "enrollments")
@Getter @Setter @NoArgsConstructor
class InscriptionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "class_id", nullable = false) private Long classId;
    @Column(name = "academic_year_id", nullable = false) private Long academicYearId;
    @Enumerated(EnumType.STRING) private EnrollmentType type;
    @Enumerated(EnumType.STRING) private EnrollmentStatus status;
    @Column(name = "enrolled_at") private LocalDate enrolledAt;
    @Column(name = "platform_access_opt_in", nullable = false) private Boolean platformAccessOptIn;
    @Column(name = "monthly_payment_opt_in", nullable = false) private Boolean monthlyPaymentOptIn;
    @Column(name = "departure_date") private LocalDate departureDate;
    @Enumerated(EnumType.STRING) @Column(name = "departure_reason") private DepartureReason departureReason;
    @Column(name = "destination_school") private String destinationSchool;
}
