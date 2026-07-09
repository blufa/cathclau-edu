package gn.lacaeducation.assignments.adapters.out.persistence;

import gn.lacaeducation.assignments.domain.SubmissionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignment_submissions")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class AssignmentSubmissionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "assignment_id", nullable = false) private Long assignmentId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private SubmissionStatus status;
    @Column(name = "file_url") private String fileUrl;
    @Column(precision = 6, scale = 2) private BigDecimal grade;
    @Column(columnDefinition = "text") private String feedback;
}
