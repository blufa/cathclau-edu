package gn.lacaeducation.academics.adapters.out.persistence;

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
 * Read-only view of the enrollments table for the academics module.
 * Only used to query student enrollment data without coupling to the students module.
 */
@Entity
@Table(name = "enrollments")
@Getter @Setter @NoArgsConstructor
class AcademicsEnrollmentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "class_id", nullable = false)
    private Long classId;
    @Column(name = "status", nullable = false)
    private String status;
}
