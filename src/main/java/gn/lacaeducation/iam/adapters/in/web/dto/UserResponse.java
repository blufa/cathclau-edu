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
        /** Dernière requête authentifiée reçue (§14 cahier des charges — "dernière connexion : date,
         * heure et durée") : throttlée à 1 écriture/minute par {@code UserActivityFilter}, jamais une
         * heure de déconnexion explicite (la plupart des sessions ne passent jamais par /auth/logout).
         * Combinée à {@code lastLoginAt}, permet au frontend de calculer une durée de session
         * approximative SANS en inventer une quand l'une des deux dates manque. */
        LocalDateTime lastActivityAt,
        LocalDateTime createdAt
) {}
