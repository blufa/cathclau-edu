package gn.lacaeducation.common;

/**
 * Public view of a tenant (school) exposed to other modules (notification, iam, billing/payments
 * PDF headers…). Owned by the platform module, which is the source of truth for tenants.
 * Includes the legal/contact details printed on official documents (logo, address, accreditation,
 * opening authorization).
 */
public record TenantPublicInfo(Long id, String schoolName, String email, String phone,
                               /** Second school phone number (optional) — see {@code Tenant#phone2}. */
                               String phone2,
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
                               String matriculeFormat, Integer matriculeDigits,
                               /** Numéro du fondateur/administrateur dédié aux alertes automatiques
                                * (écart de caisse, exonération suspecte, retard/absence enseignant…) —
                                * §retour fondateurs. Null = les alertes restent envoyées à tous les
                                * comptes SCHOOL_ADMIN du tenant (comportement historique), voir
                                * {@code CommunicationEventListener#notifySchoolAdmins}. */
                               String founderPhone,
                               /** Quand un paiement est encaissé, le parent reçoit toujours un SMS — ce
                                * champ contrôle en plus l'alerte SCHOOL_ADMIN/DIRECTEUR (anti-fraude) ET
                                * celle à la personne qui a physiquement encaissé. Réglable uniquement par
                                * le SUPER_ADMIN, désactivé par défaut (§retour comptable). */
                               boolean notifyStaffOnPayment) {}
