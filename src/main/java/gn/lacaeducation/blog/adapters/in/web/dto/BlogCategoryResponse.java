package gn.lacaeducation.blog.adapters.in.web.dto;

import gn.lacaeducation.blog.domain.BlogCategory;

public record BlogCategoryResponse(Long id, String label, String slug) {
    public static BlogCategoryResponse from(BlogCategory c) {
        return new BlogCategoryResponse(c.getId(), c.getLabel(), c.getSlug());
    }
}
