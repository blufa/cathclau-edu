package gn.lacaeducation.blog.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "blog_categories")
@Getter @Setter @NoArgsConstructor
class BlogCategoryJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String label;
    @Column(nullable = false, unique = true) private String slug;
}
