package gn.lacaeducation.blog.adapters.in.web.dto;

import gn.lacaeducation.blog.domain.BlogComment;
import gn.lacaeducation.blog.domain.CommentStatus;

import java.time.LocalDateTime;

public record BlogCommentResponse(Long id, Long postId, String authorName, String authorEmail,
                                   String website, String content, CommentStatus status, LocalDateTime createdAt) {
    public static BlogCommentResponse from(BlogComment c) {
        return new BlogCommentResponse(c.getId(), c.getPostId(), c.getAuthorName(), c.getAuthorEmail(),
                c.getWebsite(), c.getContent(), c.getStatus(), c.getCreatedAt());
    }

    /** Vue publique : jamais l'e-mail du visiteur. */
    public record Public(Long id, String authorName, String content, LocalDateTime createdAt) {
        public static Public from(BlogComment c) {
            return new Public(c.getId(), c.getAuthorName(), c.getContent(), c.getCreatedAt());
        }
    }
}
