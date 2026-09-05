package gn.lacaeducation.courses.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class CourseJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "class_id", nullable = false) private Long classId;
    @Column(name = "subject_label") private String subjectLabel;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "text") private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(name = "thumbnail_url") private String thumbnailUrl;
    @Column(name = "school_fee_id", nullable = false) private Long schoolFeeId;
    @Column(nullable = false) private boolean published;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
