package gn.lacaeducation.blog.adapters.out.persistence;

import gn.lacaeducation.blog.application.port.out.BlogPostRepository;
import gn.lacaeducation.blog.domain.BlogPost;
import gn.lacaeducation.blog.domain.PostStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class BlogPostPersistenceAdapter implements BlogPostRepository {
    private final BlogPostJpaRepository jpa;
    private final BlogPostTagJpaRepository tagJpa;

    @Override
    @Transactional
    public BlogPost save(BlogPost post) {
        BlogPostJpaEntity saved = jpa.save(toEntity(post));
        // Remplace entièrement la liste des tags à chaque sauvegarde — plus simple et sûr qu'un diff
        // incrémental pour un volume d'articles/tags attendu faible.
        tagJpa.deleteByPostId(saved.getId());
        List<String> tags = post.getTags() != null ? post.getTags() : List.of();
        tagJpa.saveAll(tags.stream().map(t -> new BlogPostTagJpaEntity(saved.getId(), t)).toList());
        return toDomain(saved, tags);
    }

    @Override public Optional<BlogPost> findById(Long id) {
        return jpa.findById(id).map(e -> toDomain(e, tagsOf(e.getId())));
    }

    @Override public Optional<BlogPost> findBySlug(String slug) {
        return jpa.findBySlug(slug).map(e -> toDomain(e, tagsOf(e.getId())));
    }

    @Override public boolean existsBySlug(String slug) { return jpa.existsBySlug(slug); }

    @Override public List<BlogPost> findAll() {
        return jpa.findAll().stream().map(e -> toDomain(e, tagsOf(e.getId()))).toList();
    }

    @Override public List<BlogPost> findByStatus(PostStatus status) {
        return jpa.findByStatus(status).stream().map(e -> toDomain(e, tagsOf(e.getId()))).toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        tagJpa.deleteByPostId(id);
        jpa.deleteById(id);
    }

    private List<String> tagsOf(Long postId) {
        return tagJpa.findByPostId(postId).stream().map(BlogPostTagJpaEntity::getTag).toList();
    }

    private BlogPost toDomain(BlogPostJpaEntity e, List<String> tags) {
        return BlogPost.builder().id(e.getId()).slug(e.getSlug()).title(e.getTitle()).excerpt(e.getExcerpt())
                .coverImageUrl(e.getCoverImageUrl()).contentHtml(e.getContentHtml()).categoryId(e.getCategoryId())
                .authorId(e.getAuthorId()).tags(new java.util.ArrayList<>(tags)).status(e.getStatus())
                .publishedAt(e.getPublishedAt()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).build();
    }

    private BlogPostJpaEntity toEntity(BlogPost p) {
        BlogPostJpaEntity e = new BlogPostJpaEntity();
        e.setId(p.getId()); e.setSlug(p.getSlug()); e.setTitle(p.getTitle()); e.setExcerpt(p.getExcerpt());
        e.setCoverImageUrl(p.getCoverImageUrl()); e.setContentHtml(p.getContentHtml());
        e.setCategoryId(p.getCategoryId()); e.setAuthorId(p.getAuthorId()); e.setStatus(p.getStatus());
        e.setPublishedAt(p.getPublishedAt()); e.setCreatedAt(p.getCreatedAt()); e.setUpdatedAt(p.getUpdatedAt());
        return e;
    }
}
