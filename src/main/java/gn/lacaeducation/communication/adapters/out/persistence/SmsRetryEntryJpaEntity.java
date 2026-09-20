package gn.lacaeducation.communication.adapters.out.persistence;

import gn.lacaeducation.communication.domain.SmsRetryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_retry_queue")
@Getter @Setter @NoArgsConstructor
public class SmsRetryEntryJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "recipient_id") private Long recipientId;
    @Column(nullable = false) private String destination;
    @Column private String subject;
    @Column(columnDefinition = "TEXT") private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SmsRetryStatus status;
    @Column(name = "attempt_count", nullable = false) private Integer attemptCount;
    @Column(name = "next_attempt_at") private LocalDateTime nextAttemptAt;
    @Column(name = "last_error", columnDefinition = "TEXT") private String lastError;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
