package gn.lacaeducation.common;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Construit un candidat de matricule (élève, enseignant ou employé) pour un tenant donné, à partir de
 * son format configuré — factorise la résolution préfixe+format+digits partagée par
 * {@code EnseignantService}, {@code EmployeeService} et les tâches de rattrapage (backfill) qui
 * assignent un matricule aux fiches qui n'en ont pas encore.
 */
public final class MatriculeAllocator {

    /** Incident de production : un format d'école sans jeton {@code {NUM}} (ex. "GSBD24 0000", saisi
     * sans le jeton) produit un candidat IDENTIQUE à chaque itération — la boucle d'unicité de
     * {@link #resolveUnique} tournait alors indéfiniment dès que ce candidat existait déjà, chaque
     * itération étant une requête SQL, saturant le CPU de la base. {@link #resolveUnique} borne
     * maintenant la recherche ; voir aussi la validation à l'enregistrement du format
     * ({@code SchoolProfileController}) qui empêche de sauvegarder un format sans {@code {NUM}}. */
    private static final int MAX_ATTEMPTS = 1000;

    private MatriculeAllocator() {}

    public static String candidate(TenantInfoProvider provider, Long tenantId, long seq) {
        String prefix = SchoolCode.prefix(provider, tenantId);
        Optional<TenantPublicInfo> info = tenantId == null ? Optional.empty() : provider.findTenantInfo(tenantId);
        String format = info.map(TenantPublicInfo::matriculeFormat).orElse(null);
        Integer digits = info.map(TenantPublicInfo::matriculeDigits).orElse(null);
        return MatriculeFormatter.generate(format, digits, prefix, seq);
    }

    /** Génère le premier candidat (à partir de la valeur courante de {@code seq}, avancée d'autant
     * d'essais qu'il en a fallu — important pour un appelant qui résout plusieurs matricules de suite
     * dans la même boucle, voir {@code StaffMatriculeBackfill} : chaque personne doit repartir où la
     * précédente s'est arrêtée, pas d'un seul cran fixe) pour lequel {@code alreadyTaken} est faux —
     * borné à {@link #MAX_ATTEMPTS} essais, voir le commentaire ci-dessus. */
    public static String resolveUnique(TenantInfoProvider provider, Long tenantId, AtomicLong seq, Predicate<String> alreadyTaken) {
        String candidate;
        int attempts = 0;
        do {
            if (++attempts > MAX_ATTEMPTS) {
                throw new BusinessRuleException(
                        "Impossible de générer un matricule unique après " + MAX_ATTEMPTS
                                + " tentatives — vérifiez le format de matricule de l'école.");
            }
            candidate = candidate(provider, tenantId, seq.getAndIncrement());
        } while (alreadyTaken.test(candidate));
        return candidate;
    }

    public static String resolveUnique(TenantInfoProvider provider, Long tenantId, long startSeq, Predicate<String> alreadyTaken) {
        return resolveUnique(provider, tenantId, new AtomicLong(startSeq), alreadyTaken);
    }
}
