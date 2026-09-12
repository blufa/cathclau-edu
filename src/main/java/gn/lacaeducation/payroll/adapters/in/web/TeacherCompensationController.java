package gn.lacaeducation.payroll.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payroll.application.service.PayrollService;
import gn.lacaeducation.payroll.application.service.PayrollService.MonthlyPay;
import gn.lacaeducation.payroll.application.service.PayrollService.TheoreticalMonthlyPay;
import gn.lacaeducation.payroll.domain.PayMode;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.TeacherCompensation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Teacher Compensation", description = "Mode de rémunération (horaire/fixe) et paie mensuelle basée sur les heures réalisées")
@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class TeacherCompensationController {

    private final PayrollService service;

    public record CompensationRequest(PayMode payMode, BigDecimal hourlyRateNormal, BigDecimal hourlyRateExam,
                                      BigDecimal fixedSalary) {}

    @Operation(summary = "Lister les rémunérations paramétrées")
    @PreAuthorize("hasAuthority('paie:manage')")
    @GetMapping("/compensation")
    public List<TeacherCompensation> list() { return service.compensations(TenantContext.get()); }

    @Operation(summary = "Rémunération d'un enseignant")
    @PreAuthorize("hasAuthority('paie:manage')")
    @GetMapping("/compensation/{teacherId}")
    public ResponseEntity<TeacherCompensation> get(@PathVariable Long teacherId) {
        return service.compensation(teacherId).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Définir la rémunération d'un enseignant (mode + tarif horaire / salaire fixe)")
    @PreAuthorize("hasAuthority('paie:manage')")
    @PutMapping("/compensation/{teacherId}")
    public TeacherCompensation set(@PathVariable Long teacherId, @RequestBody CompensationRequest r) {
        return service.setCompensation(TenantContext.get(), teacherId, r.payMode(),
                r.hourlyRateNormal(), r.hourlyRateExam(), r.fixedSalary());
    }

    @Operation(summary = "Marquer un mois précis (année + mois) comme payé pendant les vacances pour cet enseignant")
    @PreAuthorize("hasAuthority('paie:manage')")
    @PostMapping("/compensation/{teacherId}/vacation-pay")
    public gn.lacaeducation.payroll.domain.TeacherVacationPayMonth addVacationPay(
            @PathVariable Long teacherId, @RequestParam int year, @RequestParam int month) {
        return service.addVacationPayMonth(TenantContext.get(), teacherId, year, month);
    }

    @Operation(summary = "Lister les mois payés pendant les vacances pour cet enseignant")
    @PreAuthorize("hasAuthority('paie:manage')")
    @GetMapping("/compensation/{teacherId}/vacation-pay")
    public List<gn.lacaeducation.payroll.domain.TeacherVacationPayMonth> listVacationPay(@PathVariable Long teacherId) {
        return service.listVacationPayMonths(teacherId);
    }

    @Operation(summary = "Retirer un mois payé pendant les vacances")
    @PreAuthorize("hasAuthority('paie:manage')")
    @DeleteMapping("/compensation/{teacherId}/vacation-pay/{id}")
    public ResponseEntity<Void> removeVacationPay(@PathVariable Long teacherId, @PathVariable Long id) {
        service.removeVacationPayMonth(teacherId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Calcul de la paie mensuelle (heures réalisées − retards × tarif, + fixe le cas échéant)")
    @PreAuthorize("hasAuthority('paie:manage')")
    @GetMapping("/teachers/{teacherId}/monthly-pay")
    public MonthlyPay monthlyPay(@PathVariable Long teacherId, @RequestParam int year, @RequestParam int month) {
        return service.computeMonthlyPay(teacherId, year, month);
    }

    @Operation(summary = "Calcul automatique de la paie mensuelle depuis l'emploi du temps validé, par typologie de classe (§6), sans saisie manuelle")
    @PreAuthorize("hasAuthority('paie:manage')")
    @GetMapping("/teachers/{teacherId}/theoretical-pay")
    public TheoreticalMonthlyPay theoreticalPay(@PathVariable Long teacherId, @RequestParam int year, @RequestParam int month) {
        return service.computeTheoreticalMonthlyPay(teacherId, year, month);
    }

    public record GenPayslipRequest(String teacherName, Long structureId, String teacherPhone, String bankAccount) {}

    @Operation(summary = "Générer automatiquement la fiche de paie du mois (brut basé sur les heures réalisées)")
    @PreAuthorize("hasAuthority('paie:manage')")
    @PostMapping("/teachers/{teacherId}/payslip")
    public Payslip generatePayslip(@PathVariable Long teacherId, @RequestParam int year, @RequestParam int month,
                                   @RequestBody(required = false) GenPayslipRequest r) {
        return service.generateFromHours(TenantContext.get(), teacherId, r != null ? r.teacherName() : null,
                year, month, r != null ? r.structureId() : null,
                r != null ? r.teacherPhone() : null, r != null ? r.bankAccount() : null);
    }

    @Operation(summary = "Générer automatiquement la fiche de paie du mois depuis l'emploi du temps validé (§6), sans saisie manuelle")
    @PreAuthorize("hasAuthority('paie:manage')")
    @PostMapping("/teachers/{teacherId}/payslip-timetable")
    public Payslip generatePayslipFromTimetable(@PathVariable Long teacherId, @RequestParam int year, @RequestParam int month,
                                                @RequestBody(required = false) GenPayslipRequest r) {
        return service.generateFromTimetable(TenantContext.get(), teacherId, r != null ? r.teacherName() : null,
                year, month, r != null ? r.structureId() : null,
                r != null ? r.teacherPhone() : null, r != null ? r.bankAccount() : null);
    }
}
