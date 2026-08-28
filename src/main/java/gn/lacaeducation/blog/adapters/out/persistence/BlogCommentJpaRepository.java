package gn.lacaeducation.blog.adapters.out.persistence;

import gn.lacaeducation.blog.domain.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface BlogCommentJpaRepository extends JpaRepository<BlogCommentJpaEntity, Long> {
    List<BlogCommentJpaEntity> findByPostId(Long postId);
    List<BlogCommentJpaEntity> findByPostIdAndStatus(Long postId, CommentStatus status);
    List<BlogCommentJpaEntity> findByStatus(CommentStatus status);
}
