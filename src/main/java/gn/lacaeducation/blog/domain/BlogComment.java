package gn.lacaeducation.blog.domain;

import lombok.*;

import java.time.LocalDateTime;

/** Commentaire visiteur sur un article — visible publiquement seulement une fois modéré (APPROVED). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BlogComment {
    private Long id;
    private Long postId;
    private String authorName;
    private String authorEmail;
    private String website;
    private String content;
    private CommentStatus status;
    private LocalDateTime createdAt;
}
