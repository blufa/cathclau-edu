package gn.lacaeducation.grants.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Subvention scolaire (État, ONG, donateur) — recette hors frais parents. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Grant {
    private Long id;
    private Long tenantId;
    private String source;
    private String reference;
    private String purpose;
    private String academicYear;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private GrantStatus status;
    private String notes;
    private LocalDateTime createdAt;
}
