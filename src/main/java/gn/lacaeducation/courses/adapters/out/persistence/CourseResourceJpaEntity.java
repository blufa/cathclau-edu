package gn.lacaeducation.courses.adapters.out.persistence;

import gn.lacaeducation.courses.domain.CourseResourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_resources")
@Getter @Setter @NoArgsConstructor
class CourseResourceJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "course_id", nullable = false) private Long courseId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private CourseResourceType type;
    private String title;
    @Column(name = "file_url", nullable = false, length = 1000) private String fileUrl;
    @Column(name = "sort_order") private Integer sortOrder;
    @Column(name = "duration_seconds") private Integer durationSeconds;
}
