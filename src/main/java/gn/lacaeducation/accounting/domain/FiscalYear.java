package gn.lacaeducation.accounting.domain;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FiscalYear {
    private Long id;
    private Long tenantId;
    private String label;
    private LocalDate startDate;
    private LocalDate endDate;
    private FiscalYearStatus status;
    private Long academicYearId;
}
