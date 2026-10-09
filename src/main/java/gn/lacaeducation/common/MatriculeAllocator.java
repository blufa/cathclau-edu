package gn.lacaeducation.common;

import java.util.Optional;

/**
 * Construit un candidat de matricule (élève, enseignant ou employé) pour un tenant donné, à partir de
 * son format configuré — factorise la résolution préfixe+format+digits partagée par
 * {@code EnseignantService}, {@code EmployeeService} et les tâches de rattrapage (backfill) qui
 * assignent un matricule aux fiches qui n'en ont pas encore. L'appelant reste responsable de la boucle
 * d'unicité (incrémenter {@code seq} tant que le candidat existe déjà).
 */
public final class MatriculeAllocator {

    private MatriculeAllocator() {}

    public static String candidate(TenantInfoProvider provider, Long tenantId, long seq) {
        String prefix = SchoolCode.prefix(provider, tenantId);
        Optional<TenantPublicInfo> info = tenantId == null ? Optional.empty() : provider.findTenantInfo(tenantId);
        String format = info.map(TenantPublicInfo::matriculeFormat).orElse(null);
        Integer digits = info.map(TenantPublicInfo::matriculeDigits).orElse(null);
        return MatriculeFormatter.generate(format, digits, prefix, seq);
    }
}
