package gn.lacaeducation.blog.application.port.in;

import gn.lacaeducation.blog.domain.BlogCategory;
import gn.lacaeducation.blog.domain.BlogComment;
import gn.lacaeducation.blog.domain.BlogPost;
import gn.lacaeducation.blog.domain.CommentStatus;
import gn.lacaeducation.blog.domain.PostStatus;

import java.util.List;

public interface BlogUseCase {

    record CreatePostCommand(String title, String slug, String excerpt, String coverImageUrl,
                              String contentHtml, Long categoryId, List<String> tags, Long authorId) {}

    record UpdatePostCommand(String title, String slug, String excerpt, String coverImageUrl,
                              String contentHtml, Long categoryId, List<String> tags) {}

    BlogPost createPost(CreatePostCommand command);
    BlogPost updatePost(Long id, UpdatePostCommand command);
    void deletePost(Long id);
    BlogPost publishPost(Long id);
    BlogPost unpublishPost(Long id);
    BlogPost findPostByIdAdmin(Long id);
    /** Lecture publique : lève une erreur métier si l'article n'existe pas ou n'est pas publié — un
     * brouillon n'est jamais accessible par son slug côté visiteur. */
    BlogPost findPublishedPostBySlug(String slug);
    List<BlogPost> listPostsAdmin(PostStatus status);
    List<BlogPost> listPublishedPosts(Long categoryId, String tag, String search);

    BlogCategory createCategory(String label, String slug);
    BlogCategory updateCategory(Long id, String label, String slug);
    void deleteCategory(Long id);
    List<BlogCategory> listCategories();

    /** Soumission visiteur : toujours créée en statut PENDING, invisible publiquement tant qu'un
     * rédacteur ne l'a pas approuvée. Rejette si l'article n'est pas publié. */
    BlogComment submitComment(Long postId, String authorName, String authorEmail, String website, String content);
    List<BlogComment> listApprovedComments(Long postId);
    List<BlogComment> listCommentsAdmin(CommentStatus status);
    BlogComment moderateComment(Long id, CommentStatus decision);
}
