package gn.lacaeducation.blog.adapters.out.persistence;

import gn.lacaeducation.blog.application.port.out.BlogCommentRepository;
import gn.lacaeducation.blog.domain.BlogComment;
import gn.lacaeducation.blog.domain.CommentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class BlogCommentPersistenceAdapter implements BlogCommentRepository {
    private final BlogCommentJpaRepository jpa;

    @Override public BlogComment save(BlogComment c) { return toDomain(jpa.save(toEntity(c))); }
    @Override public Optional<BlogComment> findById(Long id) { return jpa.findById(id).map(this::toDomain); }
    @Override public List<BlogComment> findByPostId(Long postId) { return jpa.findByPostId(postId).stream().map(this::toDomain).toList(); }
    @Override public List<BlogComment> findByPostIdAndStatus(Long postId, CommentStatus status) {
        return jpa.findByPostIdAndStatus(postId, status).stream().map(this::toDomain).toList();
    }
    @Override public List<BlogComment> findByStatus(CommentStatus status) { return jpa.findByStatus(status).stream().map(this::toDomain).toList(); }
    @Override public List<BlogComment> findAll() { return jpa.findAll().stream().map(this::toDomain).toList(); }

    private BlogComment toDomain(BlogCommentJpaEntity e) {
        return BlogComment.builder().id(e.getId()).postId(e.getPostId()).authorName(e.getAuthorName())
                .authorEmail(e.getAuthorEmail()).website(e.getWebsite()).content(e.getContent())
                .status(e.getStatus()).createdAt(e.getCreatedAt()).build();
    }

    private BlogCommentJpaEntity toEntity(BlogComment c) {
        BlogCommentJpaEntity e = new BlogCommentJpaEntity();
        e.setId(c.getId()); e.setPostId(c.getPostId()); e.setAuthorName(c.getAuthorName());
        e.setAuthorEmail(c.getAuthorEmail()); e.setWebsite(c.getWebsite()); e.setContent(c.getContent());
        e.setStatus(c.getStatus()); e.setCreatedAt(c.getCreatedAt());
        return e;
    }
}
