package gn.lacaeducation.blog.adapters.in.web;

import gn.lacaeducation.blog.adapters.in.web.dto.BlogCategoryResponse;
import gn.lacaeducation.blog.adapters.in.web.dto.BlogCommentResponse;
import gn.lacaeducation.blog.adapters.in.web.dto.BlogPostResponse;
import gn.lacaeducation.blog.application.port.in.BlogUseCase;
import gn.lacaeducation.blog.domain.BlogPost;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Lecture publique du blog + soumission de commentaire visiteur — sans authentification.
 * Monté sous /api/v1/public/** pour hériter du permitAll() déjà en place dans SecurityConfig. */
@Tag(name = "Blog (public)", description = "Lecture publique des articles publiés et soumission de commentaires")
@RestController
@RequestMapping("/api/v1/public/blog")
@RequiredArgsConstructor
public class BlogPublicController {

    private final BlogUseCase blogUseCase;

    @Operation(summary = "Lister les articles publiés (filtrable par catégorie, tag, recherche)")
    @GetMapping("/posts")
    public List<BlogPostResponse.Summary> listPosts(@RequestParam(required = false) Long categoryId,
                                                      @RequestParam(required = false) String tag,
                                                      @RequestParam(required = false) String search) {
        return blogUseCase.listPublishedPosts(categoryId, tag, search).stream()
                .map(BlogPostResponse.Summary::from).toList();
    }

    @Operation(summary = "Détail d'un article publié par son slug (404 si brouillon ou inexistant)")
    @GetMapping("/posts/{slug}")
    public BlogPostResponse getPost(@PathVariable String slug) {
        return BlogPostResponse.from(blogUseCase.findPublishedPostBySlug(slug));
    }

    @Operation(summary = "Article précédent / suivant (par date de publication) pour la navigation")
    @GetMapping("/posts/{slug}/adjacent")
    public AdjacentResponse adjacent(@PathVariable String slug) {
        BlogPost current = blogUseCase.findPublishedPostBySlug(slug);
        List<BlogPost> ordered = blogUseCase.listPublishedPosts(null, null, null).stream()
                .sorted(Comparator.comparing(BlogPost::getPublishedAt))
                .toList();
        int idx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(current.getId())) { idx = i; break; }
        }
        BlogPostResponse.Summary previous = idx > 0 ? BlogPostResponse.Summary.from(ordered.get(idx - 1)) : null;
        BlogPostResponse.Summary next = idx >= 0 && idx < ordered.size() - 1
                ? BlogPostResponse.Summary.from(ordered.get(idx + 1)) : null;
        return new AdjacentResponse(previous, next);
    }

    @Operation(summary = "Lister les catégories")
    @GetMapping("/categories")
    public List<BlogCategoryResponse> listCategories() {
        return blogUseCase.listCategories().stream().map(BlogCategoryResponse::from).toList();
    }

    @Operation(summary = "Lister les tags distincts utilisés par les articles publiés")
    @GetMapping("/tags")
    public List<String> listTags() {
        return blogUseCase.listPublishedPosts(null, null, null).stream()
                .flatMap(p -> p.getTags() == null ? java.util.stream.Stream.<String>empty() : p.getTags().stream())
                .map(t -> t.toLowerCase(Locale.ROOT))
                .distinct().sorted().collect(Collectors.toList());
    }

    @Operation(summary = "Commentaires approuvés d'un article")
    @GetMapping("/posts/{postId}/comments")
    public List<BlogCommentResponse.Public> listComments(@PathVariable Long postId) {
        return blogUseCase.listApprovedComments(postId).stream().map(BlogCommentResponse.Public::from).toList();
    }

    @Operation(summary = "Soumettre un commentaire (visible après modération)")
    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogCommentResponse.Public submitComment(@PathVariable Long postId, @Valid @RequestBody CommentRequest req) {
        return BlogCommentResponse.Public.from(blogUseCase.submitComment(
                postId, req.authorName(), req.authorEmail(), req.website(), req.content()));
    }

    record CommentRequest(@NotBlank String authorName, @NotBlank @Email String authorEmail,
                           String website, @NotBlank String content) {}
    record AdjacentResponse(BlogPostResponse.Summary previous, BlogPostResponse.Summary next) {}
}
