package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.EstablishmentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Self-service school registration (features/01-platform.md §1, POST /public/signup). */
public record SignupRequest(
    @NotBlank String nomEcole,
    String slugSouhaite,
    // Email FACULTATIF (tout le monde n'en a pas). Validé seulement s'il est fourni.
    @Email String emailDirecteur,
    // Identifiant de connexion (username) : l'admin se connecte avec le username OU l'email.
    @NotBlank String username,
    @NotBlank @Size(min = 8) String password,
    String directeur,
    String ville,
    String pays,
    /** Un ou plusieurs types d'enseignement (primaire, collège, lycée…). */
    Set<EstablishmentType> typesEtablissement,
    String planSouhaite,
    // Compte administrateur de l'école (distinct du directeur affiché sur les documents).
    @NotBlank String prenomAdmin,
    @NotBlank String nomAdmin,
    String telephoneAdmin
) {}
