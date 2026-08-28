package gn.lacaeducation.blog.adapters.in.web;

import gn.lacaeducation.blog.adapters.in.web.dto.BlogCategoryResponse;
import gn.lacaeducation.blog.adapters.in.web.dto.BlogCommentResponse;
import gn.lacaeducation.blog.adapters.in.web.dto.BlogPostResponse;
import gn.lacaeducation.blog.application.port.in.BlogUseCase;
import gn.lacaeducation.blog.application.port.in.BlogUseCase.CreatePostCommand;
import gn.lacaeducation.blog.application.port.in.BlogUseCase.UpdatePostCommand;
import gn.lacaeducation.blog.domain.CommentStatus;
import gn.lacaeducation.blog.domain.PostStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Back-office blog : accessible à SUPER_ADMIN et au rôle restreint REDACTEUR (aucun accès
 * écoles/abonnements pour ce dernier — cf. RoleCode.REDACTEUR). */
@Tag(name = "Blog (back-office)", description = "Gestion des articles, catégories et modération des commentaires")
@RestController
@RequestMapping("/api/v1/platform/blog")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','REDACTEUR')")
@RequiredArgsConstructor
public class BlogAdminController {

    private final BlogUseCase blogUseCase;

    @Operation(summary = "Lister les articles (tous statuts)")
    @GetMapping("/posts")
    public List<BlogPostResponse> listPosts(@RequestParam(required = false) PostStatus status) {
        return blogUseCase.listPostsAdmin(status).stream().map(BlogPostResponse::from).toList();
    }

    @Operation(summary = "Détail d'un article")
    @GetMapping("/posts/{id}")
    public BlogPostResponse getPost(@PathVariable Long id) {
        return BlogPostResponse.from(blogUseCase.findPostByIdAdmin(id));
    }

    @Operation(summary = "Créer un article (brouillon)")
    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogPostResponse createPost(@RequestBody PostRequest req, @AuthenticationPrincipal Jwt jwt) {
        return BlogPostResponse.from(blogUseCase.createPost(new CreatePostCommand(
                req.title(), req.slug(), req.excerpt(), req.coverImageUrl(), req.contentHtml(),
                req.categoryId(), req.tags(), currentUserId(jwt))));
    }

    @Operation(summary = "Modifier un article")
    @PutMapping("/posts/{id}")
    public BlogPostResponse updatePost(@PathVariable Long id, @RequestBody PostRequest req) {
        return BlogPostResponse.from(blogUseCase.updatePost(id, new UpdatePostCommand(
                req.title(), req.slug(), req.excerpt(), req.coverImageUrl(), req.contentHtml(),
                req.categoryId(), req.tags())));
    }

    @Operation(summary = "Supprimer un article")
    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id) { blogUseCase.deletePost(id); }

    @Operation(summary = "Publier un article")
    @PatchMapping("/posts/{id}/publish")
    public BlogPostResponse publish(@PathVariable Long id) { return BlogPostResponse.from(blogUseCase.publishPost(id)); }

    @Operation(summary = "Dépublier un article (repasse en brouillon)")
    @PatchMapping("/posts/{id}/unpublish")
    public BlogPostResponse unpublish(@PathVariable Long id) { return BlogPostResponse.from(blogUseCase.unpublishPost(id)); }

    @Operation(summary = "Lister les catégories")
    @GetMapping("/categories")
    public List<BlogCategoryResponse> listCategories() {
        return blogUseCase.listCategories().stream().map(BlogCategoryResponse::from).toList();
    }

    @Operation(summary = "Créer une catégorie")
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogCategoryResponse createCategory(@RequestBody CategoryRequest req) {
        return BlogCategoryResponse.from(blogUseCase.createCategory(req.label(), req.slug()));
    }

    @Operation(summary = "Modifier une catégorie")
    @PutMapping("/categories/{id}")
    public BlogCategoryResponse updateCategory(@PathVariable Long id, @RequestBody CategoryRequest req) {
        return BlogCategoryResponse.from(blogUseCase.updateCategory(id, req.label(), req.slug()));
    }

    @Operation(summary = "Supprimer une catégorie")
    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) { blogUseCase.deleteCategory(id); }

    @Operation(summary = "Lister les commentaires (filtrable par statut, ex. PENDING pour la file de modération)")
    @GetMapping("/comments")
    public List<BlogCommentResponse> listComments(@RequestParam(required = false) CommentStatus status) {
        return blogUseCase.listCommentsAdmin(status).stream().map(BlogCommentResponse::from).toList();
    }

    @Operation(summary = "Approuver ou rejeter un commentaire")
    @PatchMapping("/comments/{id}/moderate")
    public BlogCommentResponse moderate(@PathVariable Long id, @RequestBody ModerateRequest req) {
        return BlogCommentResponse.from(blogUseCase.moderateComment(id, req.decision()));
    }

    private Long currentUserId(Jwt jwt) {
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }

    record PostRequest(@NotBlank String title, String slug, String excerpt, String coverImageUrl,
                        @NotBlank String contentHtml, Long categoryId, List<String> tags) {}
    record CategoryRequest(@NotBlank String label, String slug) {}
    record ModerateRequest(CommentStatus decision) {}
}
