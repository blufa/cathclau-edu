package gn.lacaeducation.studentcards.adapters.out.persistence;
import gn.lacaeducation.studentcards.domain.CardStatus;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity @Table(name = "student_card") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class StudentCardJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "card_template_id") private Long cardTemplateId;
    @Column(name = "academic_year_id") private Long academicYearId;
    @Column(name = "class_id") private Long classId;
    @Column(name = "student_number") private String studentNumber;
    @Column(name = "card_number") private String cardNumber;
    @Column(name = "qr_payload", columnDefinition = "text") private String qrPayload;
    @Column(name = "qr_signature", columnDefinition = "text") private String qrSignature;
    @Column(name = "photo_url") private String photoUrl;
    @Column(name = "issue_date") private LocalDate issueDate;
    @Column(name = "expiration_date") private LocalDate expirationDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private CardStatus status;
    @Column(name = "pdf_url") private String pdfUrl;
    @Column(name = "replaces_card_id") private Long replacesCardId;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "printed_at") private LocalDateTime printedAt;
}
