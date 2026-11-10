package gn.lacaeducation.common;

import java.util.Optional;

/**
 * Shared contract giving every module read-only access to tenant (school) information without
 * depending on the platform module's internals. Lives in {@code common} (the shared kernel, like
 * {@link TenantContext}) so it can be consumed by modules that platform itself depends on
 * (e.g. payments), avoiding a dependency cycle. Platform owns the single implementation.
 */
public interface TenantInfoProvider {

    Optional<TenantPublicInfo> findTenantInfo(Long tenantId);

    /**
     * Whether the tenant currently allows its users to access the application.
     * Returns false for suspended/archived/terminated tenants and for unknown ids.
     * A {@code null} tenantId (e.g. SUPER_ADMIN, no tenant scope) is considered active.
     */
    boolean isTenantActive(Long tenantId);

    /**
     * §censeur, école visitée : certaines écoles ne font qu'une évaluation mensuelle par matière et
     * l'utilisent directement comme moyenne de cours du mois dans le calcul de la moyenne
     * semestrielle officielle (voir {@code grading.domain.SubjectSemesterAverage#compute}).
     * Réglable par l'école elle-même ; {@code default false} pour que les implémentations de test
     * qui n'ont pas besoin de ce réglage (anonymous {@code TenantInfoProvider}) n'aient rien à
     * changer. Un tenantId inconnu ou {@code null} retourne false (comportement historique).
     */
    default boolean monthlyCountsAsCours(Long tenantId) {
        return false;
    }
}
