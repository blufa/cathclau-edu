package gn.lacaeducation.iam.adapters.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        Long tenantId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        /** Optionnel : tous les utilisateurs (parent, enseignant, personnel) se connectent par numéro
         * de téléphone — l'email n'est qu'un canal optionnel supplémentaire pour recevoir les
         * identifiants. Au moins l'un des deux (email ou phone) reste exigé (voir UserService.create). */
        @Email String email,
        /** Optionnel : si absent, un mot de passe temporaire est généré et renvoyé une seule fois. */
        String password,
        String phone
) {}
