package gn.lacaeducation.common;

/**
 * Public view of a tenant (school) exposed to other modules (notification, iam, billing/payments
 * PDF headers…). Owned by the platform module, which is the source of truth for tenants.
 * Includes the legal/contact details printed on official documents (logo, address, accreditation,
 * opening authorization).
 */
public record TenantPublicInfo(Long id, String schoolName, String email, String phone,
                               String logo, String address, String city, String country,
                               String website, String director,
                               String accreditationNumber, String openingAuthorization,
                               String brandColor,
                               /** Inspection régionale / direction préfectorale / sous-préfectorale de rattachement
                                * — en-tête officiel des documents pédagogiques (§common/pdf). */
                               String ire, String dpeLabel, String sousPrefectureLabel,
                               /** Devise de l'établissement (phrase courte, ex. "Excellence, Rigueur, Discipline")
                                * — affichée sous le nom de l'école dans l'en-tête des documents (§common/pdf). */
                               String motto,
                               /** Format de matricule défini par l'école (jetons {PREFIXE}/{ANNEE}/{NUM}) et
                                * nombre de chiffres du numéro séquentiel — null = valeurs par défaut. Voir
                                * {@link MatriculeFormatter}. */
                               String matriculeFormat, Integer matriculeDigits) {}
