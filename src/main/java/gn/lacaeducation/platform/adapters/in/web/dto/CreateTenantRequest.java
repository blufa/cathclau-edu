package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.EstablishmentType;
import gn.lacaeducation.platform.domain.TenantStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

/** Manual school creation by SUPER_ADMIN (features/01-platform.md §3). */
public record CreateTenantRequest(
    @NotBlank String schoolName,
    String slug,
    // Email facultatif (login possible par identifiant) ; validé seulement s'il est fourni.
    @Email String email,
    /** Identifiant de connexion de l'admin (login par username OU email) ; généré si vide. */
    String username,
    @NotBlank String phone,
    /** Second school phone number (optional), distinct from {@code phone} — see {@code Tenant#phone2}. */
    String phone2,
    String address,
    String city,
    String country,
    String director,
    /** Un ou plusieurs types d'enseignement (primaire, collège, lycée…). */
    Set<EstablishmentType> establishmentTypes,
    /** TRIAL (default) or ACTIVE (contract already paid). */
    TenantStatus initialState,
    /** Mot de passe initial du compte admin de l'école (optionnel ; généré aléatoirement si vide). */
    @jakarta.validation.constraints.Size(min = 8, message = "8 caractères minimum") String adminPassword
) {}
