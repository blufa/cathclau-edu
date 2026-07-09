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
    @NotBlank @Email String email,
    @NotBlank String phone,
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
