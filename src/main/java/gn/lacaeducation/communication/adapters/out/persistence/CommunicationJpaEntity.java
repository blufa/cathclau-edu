package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.domain.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "communications") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class CommunicationJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "sender_id") private Long senderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CommunicationChannel channel;
    @Enumerated(EnumType.STRING) @Column(name = "recipient_type", nullable = false) private RecipientType recipientType;
    @Column private String subject;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(nullable = false) private String content;
    @Enumerated(EnumType.STRING) @Column private CommunicationReason reason;
    @Column(name = "sent_at", nullable = false) private LocalDateTime sentAt;
}
