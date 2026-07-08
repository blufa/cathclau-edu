package gn.lacaeducation.accounting.domain;

import lombok.*;
import java.math.BigDecimal;

/** One line of a journal entry: either a debit or a credit on a chart-of-accounts line. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntryLine {
    private Long id;
    private Long journalEntryId;
    private Long chartOfAccountId;
    private String label;
    private BigDecimal debit;
    private BigDecimal credit;
    private String lettrageCode;
    private String tiersType;   // CLIENT | PARENT | SUPPLIER
    private Long tiersId;
}
