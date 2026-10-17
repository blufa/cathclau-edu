package gn.lacaeducation.platform.application.port.in;
import gn.lacaeducation.platform.domain.EstablishmentType;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.Tenant;
import gn.lacaeducation.platform.domain.TenantStatus;
import java.util.List;
import java.util.Set;
public interface CreateTenantUseCase {
    CreatedTenant create(CreateTenantCommand command);
    Tenant findById(Long id);
    TenantDetail getDetail(Long id);
    List<Tenant> findAll(String status, String city);
    Tenant update(Long id, UpdateTenantCommand command);
    /** Self-service branding update (logo + colour) by the school itself. */
    Tenant updateBranding(Long id, String logo, String brandColor);
    void suspend(Long id);
    void activate(Long id);
    /** Réservé au SUPER_ADMIN — jamais exposé via {@link #update} (utilisé aussi par l'école elle-même,
     * voir {@code SchoolProfileController}) pour qu'une école ne puisse jamais s'auto-activer cette
     * option (§retour comptable). Voir {@code Tenant#notifyStaffOnPayment}. */
    void setNotifyStaffOnPayment(Long id, boolean enabled);
    /** Generic status change for SUPER_ADMIN (ACTIVATE | SUSPEND | ARCHIVE) routed through the lifecycle. */
    void changeStatus(Long id, TenantStatus target);
    void delete(Long id);

    /** Manual creation by SUPER_ADMIN. {@code initialState} defaults to TRIAL (null → TRIAL).
     * {@code adminPhone} (numéro de connexion de l'admin) est distinct de {@code phone}/{@code phone2}
     * (numéros de l'école, affichés sur badges/documents) — voir {@code CreateTenantRequest#adminPhone}. */
    record CreateTenantCommand(String schoolName, String slug, String email, String adminPhone, String phone, String phone2,
            String address, String city, String country, String director,
            Set<EstablishmentType> establishmentTypes, TenantStatus initialState, String adminPassword,
            String adminUsername) {}

    /**
     * Résultat d'une création manuelle. {@code temporaryPassword} n'est renseigné que si aucun mot de
     * passe n'a été fourni (généré) — permet de l'afficher une fois au SUPER_ADMIN ; null sinon.
     */
    record CreatedTenant(Tenant tenant, String temporaryPassword) {}
    record UpdateTenantCommand(String schoolName, String logo, String brandColor,
            String address, String city, String country, String phone, String phone2, String email,
            String website, String director,
            /** Numéro dédié aux alertes automatiques (fondateur) ; voir {@code Tenant#founderPhone}. */
            String founderPhone,
            String accreditationNumber, String openingAuthorization,
            String payoutMerchantNumber, String payoutOperator, String payoutAccountName,
            String motto, String slogan, String ire, String dpeLabel, String sousPrefectureLabel,
            /** Format du matricule (jetons {PREFIXE}/{ANNEE}/{NUM}) et nombre de chiffres du numéro
             * séquentiel — null = valeurs par défaut. Voir {@code MatriculeFormatter}. */
            String matriculeFormat, Integer matriculeDigits) {}

    /** Tenant detail with its current subscription, plan and consumed student quota (§1.1). */
    record TenantDetail(Tenant tenant, Subscription currentSubscription, Plan plan,
            long consumedStudents, Integer studentLimit) {}
}
