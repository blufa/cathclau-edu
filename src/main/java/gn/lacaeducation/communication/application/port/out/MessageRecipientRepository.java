package gn.lacaeducation.communication.application.port.out;
import gn.lacaeducation.communication.domain.*;
import java.util.List;
import java.util.Optional;
public interface MessageRecipientRepository {
    MessageRecipient save(MessageRecipient recipient);
    Optional<MessageRecipient> findById(Long id);
    List<MessageRecipient> findByCommunicationId(Long communicationId);
    List<MessageRecipient> findByUserId(Long userId);
    List<MessageRecipient> findByUserIdAndRead(Long userId, boolean read);
    long countByUserIdAndRead(Long userId, boolean read);
    /** Pour recaler le statut de livraison depuis le webhook du fournisseur SMS. */
    Optional<MessageRecipient> findByProviderMessageId(String providerMessageId);
}
