package gn.lacaeducation.audit.adapters.in.web.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long tenantId,
        Long utilisateurId,
        /** Nom de l'acteur, résolu côté serveur — null si inconnu (job planifié, utilisateur supprimé),
         * le frontend retombe alors sur "Utilisateur #id" / "Système". */
        String utilisateurNom,
        String action,
        String ressource,
        Long ressourceId,
        /** Nom lisible de la ressource (ex. référence de facture, nom d'élève) — null si non résolue,
         * le frontend retombe alors sur "{ressource} #{ressourceId}". */
        String ressourceNom,
        String details,
        LocalDateTime createdAt
) {
}
