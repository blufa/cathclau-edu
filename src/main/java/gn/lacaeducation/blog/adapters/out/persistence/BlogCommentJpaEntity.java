package gn.lacaeducation.blog.adapters.out.persistence;

import gn.lacaeducation.blog.domain.CommentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "blog_comments")
@Getter @Setter @NoArgsConstructor
class BlogCommentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "post_id", nullable = false) private Long postId;
    @Column(name = "author_name", nullable = false) private String authorName;
    @Column(name = "author_email", nullable = false) private String authorEmail;
    private String website;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CommentStatus status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
