package gn.lacaeducation.iam.adapters.in.web.dto;

import java.util.List;

/** Profile of the authenticated user: identity + tenant + roles + permissions. */
public record MeResponse(
        Long userId,
        Long tenantId,
        String firstName,
        String lastName,
        String email,
        String phone,
        List<String> roles,
        List<String> permissions
) {}
