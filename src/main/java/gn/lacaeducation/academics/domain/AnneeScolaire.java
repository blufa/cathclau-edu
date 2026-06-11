package gn.lacaeducation.academics.domain;

import gn.lacaeducation.academics.domain.valueobject.StatutAnneeScolaire;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnneeScolaire {
    private Long id;
    private Long tenantId;
    private String label;
    private LocalDate startDate;
    private LocalDate endDate;
    private StatutAnneeScolaire status;
    private Boolean current;
    private LocalDateTime createdAt;
}
