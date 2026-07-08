package gn.lacaeducation.iam.adapters.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        Long tenantId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        /** Optionnel : si absent, un mot de passe temporaire est généré et renvoyé une seule fois. */
        String password,
        String phone
) {}
