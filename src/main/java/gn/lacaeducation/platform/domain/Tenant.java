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
    /** Second school phone number (optional) — distinct from {@code phone}, which in practice often
     * ends up holding the director's personal/login number by mistake since nothing on the create/
     * edit forms distinguishes "school number" from "the person filling the form's own number".
     * Both are printed on badges/receipts when set. */
    private String phone2;
    private String email;
    private String website;
    private String director;
    /**
     * Numéro du fondateur/administrateur dédié à la réception des alertes automatiques (écart de
     * caisse, exonération suspecte, présence sans inscription, retard/absence enseignant…) —
     * §retour fondateurs. Distinct de {@code phone} (numéro général de l'école, imprimé sur les
     * documents/badges). Null = on continue de notifier tous les comptes SCHOOL_ADMIN du tenant
     * (comportement historique), voir {@code CommunicationEventListener#notifySchoolAdmins}.
     */
    private String founderPhone;
    /** Quand un paiement est encaissé, le parent reçoit toujours un SMS — ce champ contrôle en plus
     * l'alerte SCHOOL_ADMIN/DIRECTEUR (anti-fraude) ET celle à la personne qui a physiquement encaissé
     * (son propre numéro). Réglable uniquement par le SUPER_ADMIN (§retour comptable : désactivé par
     * défaut, trop de SMS reçus par le personnel). Voir {@code CommunicationEventListener#notifyFounder}. */
    private boolean notifyStaffOnPayment;
    /** Numéro d'agrément de l'établissement (mention légale sur les documents officiels). */
    private String accreditationNumber;
    /** Référence de l'autorisation d'ouverture délivrée par le ministère. */
    private String openingAuthorization;
    /** Devise de l'établissement (phrase courte et formelle), imprimée au verso des badges/cartes. */
    private String motto;
    /** Slogan de l'établissement (accroche marketing), imprimé au verso des badges/cartes. */
    private String slogan;
    /** Inspection Régionale de l'Éducation de rattachement (ex. "KINDIA") — en-tête officiel des
     * documents pédagogiques (bulletins, fiches de note...). */
    private String ire;
    /** Direction Préfectorale de l'Éducation de rattachement (ex. "COYAH"). */
    private String dpeLabel;
    /** Direction Sous-préfectorale/communale de l'Éducation de rattachement (ex. "MANGATA"). */
    private String sousPrefectureLabel;
    /**
     * Numéro marchand de reversement de l'école (compte Mobile Money). L'encaissement des parents se
     * fait sur le compte plateforme Djomy ; ce numéro est celui sur lequel la plateforme reverse à
     * l'école les sommes qui lui reviennent. Renseignable à l'inscription et dans les paramètres.
     */
    private String payoutMerchantNumber;
    /** Opérateur du numéro marchand (ORANGE_MONEY, MTN_MONEY, WAVE…). */
    private String payoutOperator;
    /** Nom du titulaire du compte marchand (contrôle avant reversement). */
    private String payoutAccountName;
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
    /** Préfecture/commune (DPE/DCE) de rattachement — null si l'école n'est pas dans le périmètre
     * d'un partenariat DPE. Ne concerne jamais l'abonnement SaaS ni le fonctionnement normal de
     * l'école, uniquement la supervision en lecture seule par la DPE/DCE. */
    private Long prefectureId;
    /** Format du matricule (élèves/enseignants), défini librement par l'école via des jetons
     * {PREFIXE}/{ANNEE}/{NUM} (ex. "{PREFIXE}-{ANNEE}-{NUM}") — null = format par défaut
     * {@code {PREFIXE}{NUM}} (comportement historique, ex. "AT00001"). Voir
     * {@code gn.lacaeducation.common.MatriculeFormatter}. */
    private String matriculeFormat;
    /** Nombre de chiffres du numéro séquentiel du matricule (avec zéros de tête) — null = 5 par
     * défaut (comportement historique). */
    private Integer matriculeDigits;
}
