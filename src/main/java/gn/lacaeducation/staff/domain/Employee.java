package gn.lacaeducation.staff.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Membre du personnel non-enseignant (administratif, direction, soutien) — §7.1. Distinct de
 * {@link Teacher} (dossier pédagogique riche) : porte l'essentiel RH + le salaire de base mensuel
 * servant à générer la fiche de paie.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Employee {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private EmployeeCategory category;
    private String jobTitle;
    private LocalDate hireDate;
    /** Salaire de base mensuel, utilisé comme base de la fiche de paie. */
    private BigDecimal baseSalary;
    private String cnssNumber;
    private Boolean active;

    public String fullName() {
        String f = firstName != null ? firstName : "";
        String l = lastName != null ? lastName : "";
        return (f + " " + l).trim();
    }
}
