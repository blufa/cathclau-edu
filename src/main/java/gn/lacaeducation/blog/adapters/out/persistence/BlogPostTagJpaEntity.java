package gn.lacaeducation.blog.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "blog_post_tags")
@Getter @Setter @NoArgsConstructor
@IdClass(BlogPostTagJpaEntity.Key.class)
class BlogPostTagJpaEntity {
    @Id @Column(name = "post_id") private Long postId;
    @Id private String tag;

    BlogPostTagJpaEntity(Long postId, String tag) {
        this.postId = postId;
        this.tag = tag;
    }

    static class Key implements java.io.Serializable {
        Long postId;
        String tag;
        public Key() {}
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return java.util.Objects.equals(postId, k.postId) && java.util.Objects.equals(tag, k.tag);
        }
        @Override public int hashCode() { return java.util.Objects.hash(postId, tag); }
    }
}
