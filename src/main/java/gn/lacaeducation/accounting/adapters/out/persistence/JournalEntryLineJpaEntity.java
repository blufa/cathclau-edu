package gn.lacaeducation.accounting.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name = "journal_entry_line") @Getter @Setter
public class JournalEntryLineJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "journal_entry_id") private Long journalEntryId;
    @Column(name = "chart_of_account_id", nullable = false) private Long chartOfAccountId;
    private String label;
    @Column(precision = 15, scale = 2) private BigDecimal debit;
    @Column(precision = 15, scale = 2) private BigDecimal credit;
    @Column(name = "lettrage_code") private String lettrageCode;
    @Column(name = "tiers_type") private String tiersType;
    @Column(name = "tiers_id") private Long tiersId;
}
