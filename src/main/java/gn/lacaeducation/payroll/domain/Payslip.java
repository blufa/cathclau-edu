package gn.lacaeducation.payroll.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fiche de paie d'un enseignant/personnel pour une période : base + lignes → brut puis net.
 *
 * <p>Ne se rattache jamais à une ligne du budget annuel voté (module {@code expenses}) : ce budget
 * couvre le fonctionnement courant (carburant, kermesse, événements...), financièrement distinct de la
 * masse salariale, toujours puisée directement dans les recettes de l'école (frais scolaires). Compter
 * un salaire comme une dépense du budget voté a existé un temps ({@code budgetLineId}, retiré) mais
 * mélangeait deux enveloppes financières qui ne doivent jamais se confondre.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payslip {
    private Long id;
    private Long tenantId;
    /** Nature du salarié : enseignant (défaut) ou personnel non-enseignant (§7.1). */
    private EmployeeType employeeType;
    /** Identifiant du salarié dans son référentiel (teacher ou employee selon {@link #employeeType}). */
    private Long teacherId;
    private String teacherName;
    /** Téléphone et compte bancaire figés au moment de la génération (transmis par l'appelant, qui
     * connaît déjà la fiche enseignant/personnel) — pour le fichier bancaire (§payroll), sans que ce
     * module ne dépende du module staff. Le téléphone (Mobile Money) est prioritaire à l'export. */
    private String teacherPhone;
    private String bankAccount;
    private String periodLabel;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal baseSalary;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private PayslipStatus status;
    private String paymentMethod;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
