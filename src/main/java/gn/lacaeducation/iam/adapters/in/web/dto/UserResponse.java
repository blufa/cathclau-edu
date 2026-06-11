package gn.lacaeducation.iam.adapters.in.web.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        Long tenantId,
        String firstName,
        String lastName,
        String email,
        String phone,
        Boolean active,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {}
