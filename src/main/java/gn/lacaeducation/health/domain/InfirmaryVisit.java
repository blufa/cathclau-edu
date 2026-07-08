package gn.lacaeducation.health.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Un passage d'un élève à l'infirmerie. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfirmaryVisit {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private LocalDate visitDate;
    private LocalTime visitTime;
    private String reason;
    private String careGiven;
    private BigDecimal temperature;
    private VisitOutcome outcome;
    private boolean parentNotified;
    private Long recordedBy;
    private LocalDateTime createdAt;
}
