package gn.lacaeducation.announcements.adapters.out.persistence;

import gn.lacaeducation.announcements.domain.AnnouncementScope;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class AnnouncementJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "text") private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AnnouncementScope scope;
    @Column(name = "class_id") private Long classId;
    @Column(name = "student_id") private Long studentId;
    @Column(name = "author_id") private Long authorId;
    @Column(name = "author_name") private String authorName;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
