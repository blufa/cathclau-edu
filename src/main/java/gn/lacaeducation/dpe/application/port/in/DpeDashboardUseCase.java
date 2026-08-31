package gn.lacaeducation.dpe.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Tableau de bord de supervision DPE/DCE (DPE_STATISTICS_SPECIFICATION.md §A-B) — lecture seule,
 * strictement scopé à l'ensemble de tenants passé en paramètre (jamais résolu ici : le contrôleur
 * détermine ce périmètre à partir de {@code DpeScopeContext} pour un DPE_ADMIN, ou d'un
 * {@code prefectureId} explicite pour un SUPER_ADMIN en prévisualisation).
 *
 * <p>Première tranche (headcounts) : effectifs élèves/enseignants/classes. Les taux de présence, les
 * moyennes et le score de risque de décrochage nécessitent une agrégation supplémentaire non encore
 * construite (voir DPE_ARCHITECTURE_ANALYSIS.md, section « Reste à faire ») — volontairement absents
 * ici plutôt que simulés.</p>
 */
public interface DpeDashboardUseCase {

    /** Vue générale de la préfecture/commune (§7 cahier des charges, §A spécification). */
    DashboardOverview overview(Set<Long> tenantIds);

    /** Liste des écoles du périmètre avec leurs effectifs de base (pour un tableau). */
    List<SchoolSummary> schools(Set<Long> tenantIds);

    /** Détail d'une école — 403 si {@code tenantId} n'appartient pas à {@code allowedTenantIds}. */
    SchoolSummary schoolDetail(Set<Long> allowedTenantIds, Long tenantId);

    record SchoolSummary(Long tenantId, String schoolName, String city, String status,
                          long studentCount, long teacherCount, long activeTeacherCount, long classCount) {}

    record DashboardOverview(int schoolCount, long studentCount, long teacherCount, long classCount,
                              /** Ratio élèves/enseignant global — indicatif, jamais présenté comme une norme. */
                              BigDecimal studentTeacherRatio, List<SchoolSummary> schools) {}
}
