package gn.lacaeducation.blog.application.service;

import gn.lacaeducation.blog.application.port.in.BlogUseCase;
import gn.lacaeducation.blog.application.port.out.BlogCategoryRepository;
import gn.lacaeducation.blog.application.port.out.BlogCommentRepository;
import gn.lacaeducation.blog.application.port.out.BlogPostRepository;
import gn.lacaeducation.blog.domain.BlogCategory;
import gn.lacaeducation.blog.domain.BlogComment;
import gn.lacaeducation.blog.domain.BlogPost;
import gn.lacaeducation.blog.domain.CommentStatus;
import gn.lacaeducation.blog.domain.PostStatus;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class BlogService implements BlogUseCase {

    private final BlogPostRepository postRepository;
    private final BlogCategoryRepository categoryRepository;
    private final BlogCommentRepository commentRepository;

    @Override
    public BlogPost createPost(CreatePostCommand cmd) {
        String slug = resolveSlug(cmd.slug(), cmd.title());
        if (postRepository.existsBySlug(slug)) {
            throw new ConflictException("Un article utilise déjà le slug \"" + slug + "\"");
        }
        LocalDateTime now = LocalDateTime.now();
        return postRepository.save(BlogPost.builder()
                .title(cmd.title()).slug(slug).excerpt(cmd.excerpt()).coverImageUrl(cmd.coverImageUrl())
                .contentHtml(cmd.contentHtml()).categoryId(cmd.categoryId())
                .tags(cmd.tags() != null ? cmd.tags() : List.of()).authorId(cmd.authorId())
                .status(PostStatus.DRAFT).createdAt(now).updatedAt(now).build());
    }

    @Override
    public BlogPost updatePost(Long id, UpdatePostCommand cmd) {
        BlogPost post = findPostByIdAdmin(id);
        String slug = resolveSlug(cmd.slug(), cmd.title());
        if (!slug.equals(post.getSlug()) && postRepository.existsBySlug(slug)) {
            throw new ConflictException("Un article utilise déjà le slug \"" + slug + "\"");
        }
        post.setTitle(cmd.title()); post.setSlug(slug); post.setExcerpt(cmd.excerpt());
        post.setCoverImageUrl(cmd.coverImageUrl()); post.setContentHtml(cmd.contentHtml());
        post.setCategoryId(cmd.categoryId()); post.setTags(cmd.tags() != null ? cmd.tags() : List.of());
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    @Override
    public void deletePost(Long id) { postRepository.deleteById(id); }

    @Override
    public BlogPost publishPost(Long id) {
        BlogPost post = findPostByIdAdmin(id);
        post.setStatus(PostStatus.PUBLISHED);
        if (post.getPublishedAt() == null) post.setPublishedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    @Override
    public BlogPost unpublishPost(Long id) {
        BlogPost post = findPostByIdAdmin(id);
        post.setStatus(PostStatus.DRAFT);
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    @Override @Transactional(readOnly = true)
    public BlogPost findPostByIdAdmin(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("BlogPost", id));
    }

    @Override @Transactional(readOnly = true)
    public BlogPost findPublishedPostBySlug(String slug) {
        BlogPost post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable"));
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Article introuvable");
        }
        return post;
    }

    @Override @Transactional(readOnly = true)
    public List<BlogPost> listPostsAdmin(PostStatus status) {
        return status != null ? postRepository.findByStatus(status) : postRepository.findAll();
    }

    @Override @Transactional(readOnly = true)
    public List<BlogPost> listPublishedPosts(Long categoryId, String tag, String search) {
        String q = search != null ? search.trim().toLowerCase(Locale.ROOT) : null;
        String t = tag != null ? tag.trim().toLowerCase(Locale.ROOT) : null;
        return postRepository.findByStatus(PostStatus.PUBLISHED).stream()
                .filter(p -> categoryId == null || categoryId.equals(p.getCategoryId()))
                .filter(p -> t == null || (p.getTags() != null && p.getTags().stream().anyMatch(x -> x.toLowerCase(Locale.ROOT).equals(t))))
                .filter(p -> q == null || q.isBlank()
                        || p.getTitle().toLowerCase(Locale.ROOT).contains(q)
                        || (p.getExcerpt() != null && p.getExcerpt().toLowerCase(Locale.ROOT).contains(q)))
                .sorted((a, b) -> {
                    LocalDateTime da = a.getPublishedAt() != null ? a.getPublishedAt() : a.getCreatedAt();
                    LocalDateTime db = b.getPublishedAt() != null ? b.getPublishedAt() : b.getCreatedAt();
                    return db.compareTo(da);
                })
                .toList();
    }

    @Override
    public BlogCategory createCategory(String label, String slug) {
        return categoryRepository.save(BlogCategory.builder().label(label).slug(resolveSlug(slug, label)).build());
    }

    @Override
    public BlogCategory updateCategory(Long id, String label, String slug) {
        BlogCategory c = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("BlogCategory", id));
        c.setLabel(label); c.setSlug(resolveSlug(slug, label));
        return categoryRepository.save(c);
    }

    @Override
    public void deleteCategory(Long id) { categoryRepository.deleteById(id); }

    @Override @Transactional(readOnly = true)
    public List<BlogCategory> listCategories() { return categoryRepository.findAll(); }

    @Override
    public BlogComment submitComment(Long postId, String authorName, String authorEmail, String website, String content) {
        BlogPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", postId));
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new BusinessRuleException("Impossible de commenter un article non publié");
        }
        return commentRepository.save(BlogComment.builder()
                .postId(postId).authorName(authorName).authorEmail(authorEmail).website(website)
                .content(content).status(CommentStatus.PENDING).createdAt(LocalDateTime.now()).build());
    }

    @Override @Transactional(readOnly = true)
    public List<BlogComment> listApprovedComments(Long postId) {
        return commentRepository.findByPostIdAndStatus(postId, CommentStatus.APPROVED);
    }

    @Override @Transactional(readOnly = true)
    public List<BlogComment> listCommentsAdmin(CommentStatus status) {
        return status != null ? commentRepository.findByStatus(status) : commentRepository.findAll();
    }

    @Override
    public BlogComment moderateComment(Long id, CommentStatus decision) {
        if (decision != CommentStatus.APPROVED && decision != CommentStatus.REJECTED) {
            throw new BusinessRuleException("Décision de modération invalide");
        }
        BlogComment comment = commentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("BlogComment", id));
        comment.setStatus(decision);
        return commentRepository.save(comment);
    }

    /** Génère un slug à partir du titre si aucun n'est fourni explicitement (minuscules, sans accents,
     * espaces/ponctuation remplacés par des tirets). */
    private String resolveSlug(String providedSlug, String title) {
        String base = (providedSlug != null && !providedSlug.isBlank()) ? providedSlug : title;
        String normalized = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            throw new BusinessRuleException("Impossible de générer un slug à partir du titre fourni");
        }
        return slug;
    }
}
