package gn.lacaeducation.audit.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ligne d'audit append-only (DPE_ARCHITECTURE_ANALYSIS.md §16/§18). Créée exclusivement par
 * {@code AuditEventListener} à partir des événements applicatifs déjà publiés par les modules
 * métier — jamais modifiée ni supprimée après coup.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private Long id;
    /** Null pour les actions plateforme (hors tenant, ex. création d'une école). */
    private Long tenantId;
    /** Null si l'action n'est rattachable à aucun utilisateur (job planifié, callback fournisseur). */
    private Long utilisateurId;
    private String action;
    private String ressource;
    private Long ressourceId;
    private String details;
    private String adresseIp;
    private String userAgent;
    private LocalDateTime createdAt;
}
