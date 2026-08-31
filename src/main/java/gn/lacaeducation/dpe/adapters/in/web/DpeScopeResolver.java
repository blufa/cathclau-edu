package gn.lacaeducation.dpe.adapters.in.web;

import gn.lacaeducation.common.DpeScopeContext;
import gn.lacaeducation.common.DpeScopeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Résolution du périmètre cross-tenant pour les endpoints du module {@code dpe} — un seul endroit
 * pour cette décision de sécurité, partagé par {@link DpeDashboardController} et
 * {@link DpeAlertController} :
 * <ul>
 *   <li>DPE_ADMIN : toujours son propre périmètre ({@link DpeScopeContext}, résolu par
 *       {@code TenantContextFilter}) — un {@code prefectureId} de requête est ignoré pour ce rôle,
 *       jamais utilisé comme source de vérité.</li>
 *   <li>SUPER_ADMIN : doit fournir explicitement {@code prefectureId} (prévisualisation/support).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
class DpeScopeResolver {

    private final DpeScopeProvider dpeScopeProvider;

    Set<Long> resolveTenantScope(Long prefectureId, Authentication auth) {
        if (isSuperAdmin(auth)) {
            if (prefectureId == null) {
                throw new IllegalArgumentException("prefectureId requis pour un accès SUPER_ADMIN à cet endpoint DPE.");
            }
            return dpeScopeProvider.tenantIdsForPrefecture(prefectureId);
        }
        Set<Long> scope = DpeScopeContext.get();
        return scope != null ? scope : Set.of();
    }

    /** Préfecture effective pour les endpoints d'alertes (indexés par préfecture, pas par ensemble
     * de tenants) :
     * <ul>
     *   <li>SUPER_ADMIN : {@code prefectureId} requis explicitement.</li>
     *   <li>DPE_ADMIN sans {@code prefectureId} fourni : sa préfecture affectée si elle est unique ;
     *       400 si plusieurs sont affectées (doit préciser laquelle).</li>
     *   <li>DPE_ADMIN avec {@code prefectureId} fourni : validé contre ses préfectures affectées
     *       (403 sinon — jamais une préfecture qui ne lui appartient pas).</li>
     * </ul>
     */
    Long resolvePrefectureId(Long prefectureId, Authentication auth) {
        if (isSuperAdmin(auth)) {
            if (prefectureId == null) {
                throw new IllegalArgumentException("prefectureId requis pour un accès SUPER_ADMIN à cet endpoint DPE.");
            }
            return prefectureId;
        }
        Set<Long> assigned = DpeScopeContext.getPrefectureIds();
        if (assigned == null || assigned.isEmpty()) {
            return null;
        }
        if (prefectureId != null) {
            if (!assigned.contains(prefectureId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Cette préfecture/commune ne vous est pas affectée.");
            }
            return prefectureId;
        }
        if (assigned.size() > 1) {
            throw new IllegalArgumentException(
                    "Plusieurs préfectures/communes vous sont affectées : précisez prefectureId.");
        }
        return assigned.iterator().next();
    }

    private static boolean isSuperAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_SUPER_ADMIN"));
    }
}
