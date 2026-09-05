package gn.lacaeducation.courses.adapters.out.persistence;

import gn.lacaeducation.courses.domain.CoursePurchaseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_purchases")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class CoursePurchaseJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "course_id", nullable = false) private Long courseId;
    @Column(name = "invoice_id", nullable = false) private Long invoiceId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private CoursePurchaseStatus status;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "purchased_at") private LocalDateTime purchasedAt;
}
