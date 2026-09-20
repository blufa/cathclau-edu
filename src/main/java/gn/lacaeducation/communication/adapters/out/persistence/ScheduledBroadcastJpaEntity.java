package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.domain.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "scheduled_broadcasts") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class ScheduledBroadcastJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "sender_id") private Long senderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CommunicationChannel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BroadcastAudience audience;
    @Column(name = "class_id") private Long classId;
    @Column(name = "student_id") private Long studentId;
    @Column private String subject;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(nullable = false) private String content;
    @Enumerated(EnumType.STRING) @Column private CommunicationReason reason;
    @Column(name = "send_at", nullable = false) private LocalDateTime sendAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ScheduledBroadcastStatus status;
    @Column(name = "school_event_id") private Long schoolEventId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "sent_at") private LocalDateTime sentAt;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(name = "error_message") private String errorMessage;
}
