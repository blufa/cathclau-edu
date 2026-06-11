package gn.lacaeducation.communication.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface MessageRecipientJpaRepository extends JpaRepository<MessageRecipientJpaEntity, Long> {
    List<MessageRecipientJpaEntity> findByCommunicationId(Long communicationId);
    List<MessageRecipientJpaEntity> findByUserId(Long userId);
    List<MessageRecipientJpaEntity> findByUserIdAndRead(Long userId, boolean read);
    long countByUserIdAndRead(Long userId, boolean read);
}
