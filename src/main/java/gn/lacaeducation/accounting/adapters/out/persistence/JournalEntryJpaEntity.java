package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.domain.JournalEntryStatus;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name = "journal_entry") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class JournalEntryJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "fiscal_year_id") private Long fiscalYearId;
    @Column(name = "journal_id") private Long journalId;
    @Column(name = "piece_number") private String pieceNumber;
    private LocalDate date;
    private String label;
    @Column(name = "reference_type") private String referenceType;
    @Column(name = "reference_id") private Long referenceId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private JournalEntryStatus status;
    @Column(name = "total_debit", precision = 15, scale = 2) private BigDecimal totalDebit;
    @Column(name = "total_credit", precision = 15, scale = 2) private BigDecimal totalCredit;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "journal_entry_id")
    private List<JournalEntryLineJpaEntity> lines = new ArrayList<>();
}
