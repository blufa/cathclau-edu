package gn.lacaeducation.accounting.domain;

import lombok.*;

/** An accounting journal (VT, AC, BQ, CA, OD). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Journal {
    private Long id;
    private Long tenantId;
    private String code;
    private String label;
    private String type;
    private Long defaultCounterpartAccountId;
}
