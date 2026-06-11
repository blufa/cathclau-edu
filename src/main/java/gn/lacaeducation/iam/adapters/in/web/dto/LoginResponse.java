package gn.lacaeducation.iam.adapters.in.web.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        Long userId,
        Long tenantId,
        List<String> roles
) {}
