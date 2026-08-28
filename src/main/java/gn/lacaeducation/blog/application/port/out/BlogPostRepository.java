package gn.lacaeducation.blog.application.port.out;

import gn.lacaeducation.blog.domain.BlogPost;
import gn.lacaeducation.blog.domain.PostStatus;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository {
    BlogPost save(BlogPost post);
    Optional<BlogPost> findById(Long id);
    Optional<BlogPost> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<BlogPost> findAll();
    /** Filtrage catégorie/tag/recherche appliqué en mémoire côté service (volume attendu faible pour un
     * blog d'entreprise) — évite de multiplier les requêtes JPA spécialisées pour chaque combinaison. */
    List<BlogPost> findByStatus(PostStatus status);
    void deleteById(Long id);
}
