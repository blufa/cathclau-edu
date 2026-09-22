package gn.lacaeducation.communication.application.port.in;

import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.Communication;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;

/** Diffusion de masse segmentée (§11.1) : résout les destinataires réels selon l'audience choisie
 * (tous les parents, classe, garçons/filles d'une classe, tous les enseignants, parent(s) d'un élève)
 * puis dispatche via {@link CommunicationUseCase}. */
public interface BroadcastUseCase {
    record BroadcastCommand(Long tenantId, Long senderId, CommunicationChannel channel,
                            BroadcastAudience audience, Long classId, Long studentId,
                            String subject, String content, CommunicationReason reason,
                            /** Clé d'idempotence fournie par le client (double-clic, requête réseau
                             * dupliquée) — voir {@code CommunicationUseCase.SendCommunicationCommand}. */
                            String idempotencyKey) {
        /** Sans clé d'idempotence — diffusions déclenchées par un événement métier interne (relance
         * d'échéance, événement calendrier...), qui n'en ont pas besoin. */
        public BroadcastCommand(Long tenantId, Long senderId, CommunicationChannel channel,
                                 BroadcastAudience audience, Long classId, Long studentId,
                                 String subject, String content, CommunicationReason reason) {
            this(tenantId, senderId, channel, audience, classId, studentId, subject, content, reason, null);
        }
    }

    /** {@code null} si aucun destinataire joignable n'a été résolu pour l'audience demandée. */
    Communication broadcast(BroadcastCommand command);
}
