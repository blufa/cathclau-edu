package gn.lacaeducation.blog.domain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Article du blog d'entreprise CathClau Edu — contenu plateforme, indépendant de tout tenant. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BlogPost {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String coverImageUrl;
    /** HTML produit par l'éditeur riche (TipTap) — rédigé uniquement par des comptes Rédacteur/SUPER_ADMIN. */
    private String contentHtml;
    private Long categoryId;
    private Long authorId;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private PostStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
