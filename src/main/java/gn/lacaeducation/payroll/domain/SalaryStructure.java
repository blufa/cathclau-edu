package gn.lacaeducation.payroll.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Structure salariale réutilisable : un ensemble de règles (primes/retenues) appliquées à une paie. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryStructure {
    private Long id;
    private Long tenantId;
    private String name;
    private String code;
    private boolean active;
}
