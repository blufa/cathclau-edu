package gn.lacaeducation.communication.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Diffusion de masse programmée : au lieu d'envoyer immédiatement, le message est déclenché à
 * {@code sendAt} par {@code ScheduledBroadcastDispatchScheduler}. Typiquement créée en même temps qu'un
 * événement école (§academics {@code SchoolEvent}), d'où {@code schoolEventId} — purement informatif,
 * jamais de contrainte FK (les deux modules restent indépendants).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledBroadcast {
    private Long id;
    private Long tenantId;
    private Long senderId;
    private CommunicationChannel channel;
    private BroadcastAudience audience;
    private Long classId;
    private Long studentId;
    private String subject;
    private String content;
    private CommunicationReason reason;
    private LocalDateTime sendAt;
    private ScheduledBroadcastStatus status;
    /** Référence libre vers l'événement école à l'origine de cette diffusion — informatif uniquement. */
    private Long schoolEventId;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String errorMessage;
}
