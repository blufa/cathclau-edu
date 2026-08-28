package gn.lacaeducation.blog.application.port.out;

import gn.lacaeducation.blog.domain.BlogComment;
import gn.lacaeducation.blog.domain.CommentStatus;

import java.util.List;
import java.util.Optional;

public interface BlogCommentRepository {
    BlogComment save(BlogComment comment);
    Optional<BlogComment> findById(Long id);
    List<BlogComment> findByPostId(Long postId);
    List<BlogComment> findByPostIdAndStatus(Long postId, CommentStatus status);
    List<BlogComment> findByStatus(CommentStatus status);
    List<BlogComment> findAll();
}
