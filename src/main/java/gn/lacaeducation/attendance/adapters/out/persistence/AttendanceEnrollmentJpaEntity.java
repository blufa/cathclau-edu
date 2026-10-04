package gn.lacaeducation.attendance.adapters.out.persistence;

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
 * Vue en lecture seule de la table enrollments, utilisée exclusivement par le module attendance
 * pour détecter une présence sans inscription déclarée, sans dépendre des beans du module students.
 */
@Entity
@Table(name = "enrollments")
@Getter @Setter @NoArgsConstructor
class AttendanceEnrollmentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "status", nullable = false)
    private String status;
}
