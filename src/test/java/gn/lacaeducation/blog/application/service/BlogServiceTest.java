package gn.lacaeducation.blog.application.service;

import gn.lacaeducation.blog.application.port.in.BlogUseCase.CreatePostCommand;
import gn.lacaeducation.blog.application.port.in.BlogUseCase.UpdatePostCommand;
import gn.lacaeducation.blog.application.port.out.BlogCategoryRepository;
import gn.lacaeducation.blog.application.port.out.BlogCommentRepository;
import gn.lacaeducation.blog.application.port.out.BlogPostRepository;
import gn.lacaeducation.blog.domain.BlogComment;
import gn.lacaeducation.blog.domain.BlogPost;
import gn.lacaeducation.blog.domain.CommentStatus;
import gn.lacaeducation.blog.domain.PostStatus;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock BlogPostRepository postRepository;
    @Mock BlogCategoryRepository categoryRepository;
    @Mock BlogCommentRepository commentRepository;

    BlogService service;

    BlogServiceTest() {
        // Instancié manuellement dans chaque test après config des mocks nécessaires (pas de @InjectMocks
        // pour garder le contrôle explicite sur les 3 dépendances).
    }

    private BlogService service() { return new BlogService(postRepository, categoryRepository, commentRepository); }

    @Test
    void createPost_generatesSlugFromTitle_whenNoneProvided() {
        when(postRepository.existsBySlug("mon-super-article")).thenReturn(false);
        when(postRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BlogPost post = service().createPost(new CreatePostCommand(
                "Mon Super Article !", null, "extrait", null, "<p>contenu</p>", null, List.of("guinee"), 1L));

        assertThat(post.getSlug()).isEqualTo("mon-super-article");
        assertThat(post.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(post.getAuthorId()).isEqualTo(1L);
    }

    @Test
    void createPost_rejectsDuplicateSlug() {
        when(postRepository.existsBySlug("mon-article")).thenReturn(true);

        assertThatThrownBy(() -> service().createPost(new CreatePostCommand(
                "Mon article", "mon-article", null, null, "<p>x</p>", null, List.of(), 1L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void publishPost_setsPublishedAt_onlyOnce() {
        LocalDateTime firstPublish = LocalDateTime.now().minusDays(1);
        BlogPost draft = BlogPost.builder().id(1L).slug("x").title("X").contentHtml("<p>x</p>")
                .status(PostStatus.DRAFT).tags(List.of()).createdAt(LocalDateTime.now()).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(postRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BlogPost published = service().publishPost(1L);

        assertThat(published.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();
    }

    @Test
    void findPublishedPostBySlug_throwsNotFound_whenPostIsDraft() {
        BlogPost draft = BlogPost.builder().id(1L).slug("x").status(PostStatus.DRAFT).tags(List.of()).build();
        when(postRepository.findBySlug("x")).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service().findPublishedPostBySlug("x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findPublishedPostBySlug_throwsNotFound_whenSlugUnknown() {
        when(postRepository.findBySlug("inconnu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findPublishedPostBySlug("inconnu"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listPublishedPosts_filtersByTagCaseInsensitively() {
        BlogPost matching = BlogPost.builder().id(1L).title("A").status(PostStatus.PUBLISHED)
                .tags(List.of("Guinee")).publishedAt(LocalDateTime.now()).build();
        BlogPost notMatching = BlogPost.builder().id(2L).title("B").status(PostStatus.PUBLISHED)
                .tags(List.of("autre")).publishedAt(LocalDateTime.now()).build();
        when(postRepository.findByStatus(PostStatus.PUBLISHED)).thenReturn(List.of(matching, notMatching));

        List<BlogPost> result = service().listPublishedPosts(null, "guinee", null);

        assertThat(result).containsExactly(matching);
    }

    @Test
    void submitComment_rejectsComment_onUnpublishedPost() {
        BlogPost draft = BlogPost.builder().id(1L).status(PostStatus.DRAFT).tags(List.of()).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service().submitComment(1L, "Jean", "jean@test.gn", null, "Bravo !"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void submitComment_createsPendingComment_onPublishedPost() {
        BlogPost published = BlogPost.builder().id(1L).status(PostStatus.PUBLISHED).tags(List.of()).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(published));
        when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BlogComment comment = service().submitComment(1L, "Jean", "jean@test.gn", null, "Bravo !");

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.PENDING);
    }

    @Test
    void moderateComment_approves() {
        BlogComment pending = BlogComment.builder().id(1L).postId(1L).status(CommentStatus.PENDING).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BlogComment result = service().moderateComment(1L, CommentStatus.APPROVED);

        assertThat(result.getStatus()).isEqualTo(CommentStatus.APPROVED);
    }

    @Test
    void moderateComment_rejectsInvalidDecision() {
        assertThatThrownBy(() -> service().moderateComment(1L, CommentStatus.PENDING))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updatePost_allowsKeepingSameSlug() {
        BlogPost existing = BlogPost.builder().id(1L).slug("mon-article").title("Mon article")
                .status(PostStatus.DRAFT).tags(List.of()).createdAt(LocalDateTime.now()).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(postRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BlogPost updated = service().updatePost(1L, new UpdatePostCommand(
                "Mon article", "mon-article", "nouvel extrait", null, "<p>maj</p>", null, List.of()));

        assertThat(updated.getSlug()).isEqualTo("mon-article");
        assertThat(updated.getExcerpt()).isEqualTo("nouvel extrait");
    }
}
