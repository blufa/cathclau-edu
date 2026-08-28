package gn.lacaeducation.blog.adapters.out.persistence;

import gn.lacaeducation.blog.domain.PostStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "blog_posts")
@Getter @Setter @NoArgsConstructor
class BlogPostJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String slug;
    @Column(nullable = false) private String title;
    private String excerpt;
    @Column(name = "cover_image_url") private String coverImageUrl;
    @Column(name = "content_html", nullable = false, columnDefinition = "TEXT") private String contentHtml;
    @Column(name = "category_id") private Long categoryId;
    @Column(name = "author_id", nullable = false) private Long authorId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PostStatus status;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}
