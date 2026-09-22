package gn.lacaeducation.communication.application.port.in;
import gn.lacaeducation.communication.domain.*;
import java.util.List;
import java.util.Optional;
public interface CommunicationUseCase {
    /** A single target: an optional user account and the resolved destination (phone/email) to dispatch to. */
    record RecipientTarget(Long userId, String destination) {}
    record SendCommunicationCommand(Long tenantId, Long senderId, CommunicationChannel channel,
                                    RecipientType recipientType, String subject, String content,
                                    CommunicationReason reason, List<RecipientTarget> recipients,
                                    /** Clé d'idempotence fournie par le client (double-clic, requête
                                     * réseau dupliquée) — null pour les envois déclenchés par un
                                     * événement métier interne, qui n'en ont pas besoin. */
                                    String idempotencyKey) {
        /** Sans clé d'idempotence — envois automatiques déclenchés par un événement métier interne. */
        public SendCommunicationCommand(Long tenantId, Long senderId, CommunicationChannel channel,
                                         RecipientType recipientType, String subject, String content,
                                         CommunicationReason reason, List<RecipientTarget> recipients) {
            this(tenantId, senderId, channel, recipientType, subject, content, reason, recipients, null);
        }
    }
    Communication send(SendCommunicationCommand command);
    Optional<Communication> findById(Long id);
    List<Communication> findByTenantId(Long tenantId);
    List<Communication> findBySenderId(Long senderId);
    List<Communication> findByTenantIdAndChannel(Long tenantId, CommunicationChannel channel);
    List<Communication> findByTenantIdAndReason(Long tenantId, CommunicationReason reason);
    void delete(Long id);
}
