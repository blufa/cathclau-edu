package gn.lacaeducation.blog.application.port.out;

import gn.lacaeducation.blog.domain.BlogCategory;

import java.util.List;
import java.util.Optional;

public interface BlogCategoryRepository {
    BlogCategory save(BlogCategory category);
    Optional<BlogCategory> findById(Long id);
    List<BlogCategory> findAll();
    void deleteById(Long id);
}
