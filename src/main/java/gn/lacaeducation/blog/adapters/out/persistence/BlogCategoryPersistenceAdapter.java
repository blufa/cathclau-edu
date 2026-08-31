package gn.lacaeducation.blog.adapters.out.persistence;

import gn.lacaeducation.blog.application.port.out.BlogCategoryRepository;
import gn.lacaeducation.blog.domain.BlogCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class BlogCategoryPersistenceAdapter implements BlogCategoryRepository {
    private final BlogCategoryJpaRepository jpa;

    @Override public BlogCategory save(BlogCategory c) { return toDomain(jpa.save(toEntity(c))); }
    @Override public Optional<BlogCategory> findById(Long id) { return jpa.findById(id).map(this::toDomain); }
    @Override public List<BlogCategory> findAll() { return jpa.findAll().stream().map(this::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }

    private BlogCategory toDomain(BlogCategoryJpaEntity e) {
        return BlogCategory.builder().id(e.getId()).label(e.getLabel()).slug(e.getSlug()).build();
    }

    private BlogCategoryJpaEntity toEntity(BlogCategory c) {
        BlogCategoryJpaEntity e = new BlogCategoryJpaEntity();
        e.setId(c.getId()); e.setLabel(c.getLabel()); e.setSlug(c.getSlug());
        return e;
    }
}
