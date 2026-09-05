package gn.lacaeducation.communication.domain;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageRecipient {
    private Long id;
    private Long communicationId;
    private Long userId;
    private String destination;
    private DeliveryStatus deliveryStatus;
    private Boolean read;
    private LocalDateTime deliveredAt;
    /** Identifiant du message chez le fournisseur (ex. "messageid" NimbaSMS) — permet de recaler le
     * statut quand le fournisseur rappelle via webhook après l'envoi initial. */
    private String providerMessageId;
}
