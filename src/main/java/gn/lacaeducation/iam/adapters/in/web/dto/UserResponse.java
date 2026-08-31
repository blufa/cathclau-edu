package gn.lacaeducation.iam.adapters.in.web.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        Long tenantId,
        String firstName,
        String lastName,
        String username,
        String email,
        String phone,
        String photo,
        Boolean active,
        boolean mustChangePassword,
        /** Présent uniquement dans la réponse de création quand un mot de passe temporaire a été généré. */
        String temporaryPassword,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {}
