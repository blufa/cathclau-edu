package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {
    private Long id;
    private String schoolName;
    /** Unique global slug used for the public login/branding URL (features/01-platform.md §1). */
    private String slug;
    private String logo;
    private String brandColor;
    private String address;
    private String city;
    private String country;
    private String phone;
    private String email;
    private String website;
    private String director;
    /** Numéro d'agrément de l'établissement (mention légale sur les documents officiels). */
    private String accreditationNumber;
    /** Référence de l'autorisation d'ouverture délivrée par le ministère. */
    private String openingAuthorization;
    /** Types d'enseignement de l'école (une école peut cumuler primaire, collège, lycée…). */
    @Builder.Default
    private Set<EstablishmentType> establishmentTypes = new LinkedHashSet<>();
    private String currentAcademicYear;
    private TenantStatus status;
    /** Paid plan chosen at signup (plan code), used to auto-create the subscription after verification. */
    private String requestedPlanCode;
    /** When the free trial ends (set at signup, now + trial_days). */
    private LocalDateTime trialEndsAt;
    /** When the school became usable (email verified → TRIAL, or manual activation). */
    private LocalDateTime activatedAt;
    /** When automatic onboarding completed — guards against re-running onboarding (idempotency). */
    private LocalDateTime onboardedAt;
    private LocalDateTime createdAt;
}
