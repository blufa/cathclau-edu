package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Entity @Table(name = "invoices") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class InvoiceJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "fee_id") private Long feeId;
    @Column(name = "academic_year_id") private Long academicYearId;
    @Column(unique = true, nullable = false) private String reference;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2) private BigDecimal totalAmount;
    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2) private BigDecimal paidAmount;
    @Column(name = "remaining_balance", nullable = false, precision = 15, scale = 2) private BigDecimal remainingBalance;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private InvoiceStatus status;
    @Column(name = "issue_date", nullable = false) private LocalDate issueDate;
    @Column(name = "pdf_file", length = 1024) private String pdfFile;
    @Column(name = "installment_id") private Long installmentId;
    @Column(name = "period_label", length = 60) private String periodLabel;
    @Column(name = "discount_amount", precision = 15, scale = 2) private BigDecimal discountAmount;
    @Column(name = "discount_reason", length = 255) private String discountReason;
    @Enumerated(EnumType.STRING) @Column(name = "discount_source", length = 20)
    private gn.lacaeducation.billing.domain.DiscountSource discountSource;
    @Enumerated(EnumType.STRING) @Column(name = "recovery_stage", length = 16) private gn.lacaeducation.billing.domain.RecoveryStage recoveryStage;
    @Column(name = "last_reminder_at") private java.time.LocalDate lastReminderAt;
    @Column(name = "platform_fee_amount", precision = 15, scale = 2) private BigDecimal platformFeeAmount;
    @Convert(converter = InstallmentMonthsConverter.class)
    @Column(name = "paid_months")
    private List<String> paidMonths;
    /** Utilisateur ayant physiquement encaissé le dernier paiement appliqué à cette facture (canal
     * CASH uniquement) — affiché en pied de facture PDF ("Encaissé par"). Null tant qu'aucun paiement
     * guichet n'a encore été appliqué. */
    @Column(name = "collected_by_user_id") private Long collectedByUserId;
    @Column(name = "credited_amount", precision = 15, scale = 2) private BigDecimal creditedAmount;
    @Column(name = "credit_reason", length = 255) private String creditReason;
}
