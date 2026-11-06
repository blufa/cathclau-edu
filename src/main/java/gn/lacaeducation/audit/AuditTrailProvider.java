package gn.lacaeducation.audit;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API publique du module audit pour le reporting cross-module (§retour comptable : "je veux savoir
 * les élèves transférés par jour") — jusqu'ici aucune trace datée d'un transfert de classe n'existait
 * nulle part (l'inscription est modifiée en place, sans historique propre). Le détail brut
 * ({@code AuditLog#getDetails}, format "clé=valeur;...") reste interne à ce module ; les autres
 * modules ne reçoivent que des champs déjà typés/parsés.
 */
public interface AuditTrailProvider {

    record ClassTransferAudit(Long studentId, Long oldClassId, Long newClassId, LocalDateTime occurredAt) {}

    /** Transferts de classe journalisés sur la plage, tenant courant — seulement ceux enregistrés
     * depuis ce correctif (voir AuditEventListener#onEnrollmentClassChanged). */
    List<ClassTransferAudit> findClassTransfersBetween(LocalDateTime from, LocalDateTime to);
}
