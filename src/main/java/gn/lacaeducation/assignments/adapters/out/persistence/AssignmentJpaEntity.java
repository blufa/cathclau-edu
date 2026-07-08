package gn.lacaeducation.assignments.adapters.out.persistence;

import gn.lacaeducation.assignments.domain.AssignmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class AssignmentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "class_id", nullable = false) private Long classId;
    @Column(name = "subject_id", nullable = false) private Long subjectId;
    @Column(name = "teacher_id") private Long teacherId;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "text") private String instructions;
    @Column(name = "attachment_url") private String attachmentUrl;
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(name = "max_score", precision = 6, scale = 2) private BigDecimal maxScore;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private AssignmentStatus status;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
