package gn.lacaeducation.blog.adapters.out.persistence;

import gn.lacaeducation.blog.domain.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface BlogPostJpaRepository extends JpaRepository<BlogPostJpaEntity, Long> {
    Optional<BlogPostJpaEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<BlogPostJpaEntity> findByStatus(PostStatus status);
}

interface BlogPostTagJpaRepository extends JpaRepository<BlogPostTagJpaEntity, BlogPostTagJpaEntity.Key> {
    List<BlogPostTagJpaEntity> findByPostId(Long postId);
    void deleteByPostId(Long postId);
}
