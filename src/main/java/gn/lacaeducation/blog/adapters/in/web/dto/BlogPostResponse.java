package gn.lacaeducation.blog.adapters.in.web.dto;

import gn.lacaeducation.blog.domain.BlogPost;
import gn.lacaeducation.blog.domain.PostStatus;

import java.time.LocalDateTime;
import java.util.List;

public record BlogPostResponse(Long id, String slug, String title, String excerpt, String coverImageUrl,
                                String contentHtml, Long categoryId, Long authorId, List<String> tags,
                                PostStatus status, LocalDateTime publishedAt, LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
    public static BlogPostResponse from(BlogPost p) {
        return new BlogPostResponse(p.getId(), p.getSlug(), p.getTitle(), p.getExcerpt(), p.getCoverImageUrl(),
                p.getContentHtml(), p.getCategoryId(), p.getAuthorId(), p.getTags(), p.getStatus(),
                p.getPublishedAt(), p.getCreatedAt(), p.getUpdatedAt());
    }

    /** Version allégée pour les listes (cartes) : sans le contenu HTML complet. */
    public record Summary(Long id, String slug, String title, String excerpt, String coverImageUrl,
                           Long categoryId, List<String> tags, PostStatus status, LocalDateTime publishedAt) {
        public static Summary from(BlogPost p) {
            return new Summary(p.getId(), p.getSlug(), p.getTitle(), p.getExcerpt(), p.getCoverImageUrl(),
                    p.getCategoryId(), p.getTags(), p.getStatus(), p.getPublishedAt());
        }
    }
}
