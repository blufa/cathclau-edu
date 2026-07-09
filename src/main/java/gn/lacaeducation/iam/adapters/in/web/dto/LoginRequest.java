package gn.lacaeducation.iam.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * {@code identifier} : username (comptes créés par l'école) ou email (self-service).
 * Les clés JSON {@code email} et {@code username} sont acceptées comme alias pour compatibilité front.
 */
public record LoginRequest(
        @NotBlank @JsonAlias({"email", "username"}) String identifier,
        @NotBlank String password
) {}
