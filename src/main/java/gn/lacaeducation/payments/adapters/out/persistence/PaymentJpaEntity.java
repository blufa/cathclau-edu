package gn.lacaeducation.payments.adapters.out.persistence;
import gn.lacaeducation.payments.domain.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Entity @Table(name = "payments") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class PaymentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "invoice_id") private Long invoiceId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "academic_year_id") private Long academicYearId;
    @Column(unique = true, nullable = false) private String reference;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentChannel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus status;
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(name = "covered_months", length = 500) private String coveredMonths;
    @Column(name = "months_count") private Integer monthsCount;
    @Column(name = "remaining_after", precision = 15, scale = 2) private BigDecimal remainingAfter;
    @Column(name = "group_lines", columnDefinition = "text") private String groupLines;
    @Column(name = "collected_by_user_id") private Long collectedByUserId;
    /** Élèves couverts par un paiement groupé multi-enfants (§payments — fratrie) ; [studentId] pour un paiement mono-élève. */
    @Convert(converter = PaymentStudentIdsConverter.class)
    @Column(name = "student_ids")
    private List<Long> studentIds;
}
