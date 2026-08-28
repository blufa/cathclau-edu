package gn.lacaeducation.platform;

import java.util.List;

/** API publique du module platform : liste des tenants actifs, pour les jobs planifiés d'autres
 * modules qui doivent boucler par établissement (ex. attendance — marquage automatique des absences
 * en fin de journée, hors contexte de requête donc sans tenant déjà résolu). */
public interface TenantListPort {
    record TenantRef(Long id) {}

    /** Tenants dont le statut permet une activité normale (actif ou en essai) — inutile de traiter
     * l'assiduité d'une école suspendue, expirée ou archivée. */
    List<TenantRef> listActiveTenantIds();
}
