package gn.lacaeducation.iam.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Vue plateforme (SUPER_ADMIN) d'un utilisateur, toutes écoles confondues — le nom de l'école est
 * résolu côté frontend à partir de {@code tenantId} (liste déjà chargée par /platform/tenants), pour
 * ne pas faire dépendre le module iam du module platform. */
public record PlatformUserResponse(
        Long id,
        Long tenantId,
        String firstName,
        String lastName,
        String username,
        String email,
        String phone,
        Boolean active,
        List<String> roles,
        LocalDateTime lastLoginAt,
        LocalDateTime lastActivityAt,
        /** Actif dans les 5 dernières minutes (dérivé de {@code lastActivityAt}, throttlé côté filtre). */
        boolean online
) {}
