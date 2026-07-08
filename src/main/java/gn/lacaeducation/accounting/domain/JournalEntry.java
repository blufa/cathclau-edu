package gn.lacaeducation.accounting.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** An accounting voucher: balanced debit/credit lines posted to a journal within a fiscal year. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntry {
    private Long id;
    private Long tenantId;
    private Long fiscalYearId;
    private Long journalId;
    private String pieceNumber;
    private LocalDate date;
    private String label;
    private String referenceType;
    private Long referenceId;
    private JournalEntryStatus status;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    @Builder.Default
    private List<JournalEntryLine> lines = new ArrayList<>();
}
