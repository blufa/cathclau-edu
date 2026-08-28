package gn.lacaeducation.blog.domain;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BlogCategory {
    private Long id;
    private String label;
    private String slug;
}
