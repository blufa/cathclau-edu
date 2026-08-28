package gn.lacaeducation.platform;

import java.util.Set;

/**
 * Provided port (partie de l'API publique du module platform) exposant les types d'établissement
 * d'un tenant sans que le module appelant dépende du domaine platform — retourne les noms bruts
 * ({@link Enum#name()}, ex. "PRIMAIRE", "MATERNELLE", "MIXTE") plutôt que l'enum {@code EstablishmentType}
 * pour éviter tout couplage de type inter-module (voir academics.application.service.ClasseService,
 * qui compare ces noms au cycle scolaire du niveau demandé).
 */
public interface TenantEstablishmentTypeService {
    /** Ensemble vide si le tenant n'a encore aucun type renseigné — dans ce cas, aucune restriction
     * n'est appliquée (école pas encore configurée). */
    Set<String> findEstablishmentTypeNames(Long tenantId);
}
