package gn.lacaeducation.billing.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Read-only view of the enrollments table, used exclusively by the billing module
 * to query validated student enrollments without coupling to the students module.
 */
@Entity
@Table(name = "enrollments")
@Getter @Setter @NoArgsConstructor
class BillingEnrollmentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "class_id", nullable = false)
    private Long classId;
    @Column(name = "status", nullable = false)
    private String status;
    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;
    @Column(name = "platform_access_opt_in", nullable = false)
    private Boolean platformAccessOptIn;
}
