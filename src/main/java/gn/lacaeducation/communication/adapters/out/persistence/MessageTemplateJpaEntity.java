package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "message_templates") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class MessageTemplateJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CommunicationReason reason;
    @Enumerated(EnumType.STRING) @Column private CommunicationChannel channel;
    @Column private String subject;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(nullable = false) private String body;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
