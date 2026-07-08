package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.EstablishmentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-service school registration (features/01-platform.md §1, POST /public/signup). */
public record SignupRequest(
    @NotBlank String nomEcole,
    String slugSouhaite,
    @NotBlank @Email String emailDirecteur,
    @NotBlank @Size(min = 8) String password,
    String directeur,
    String ville,
    String pays,
    EstablishmentType typeEtablissement,
    String planSouhaite,
    // Compte administrateur de l'école (distinct du directeur affiché sur les documents).
    @NotBlank String prenomAdmin,
    @NotBlank String nomAdmin,
    String telephoneAdmin
) {}
