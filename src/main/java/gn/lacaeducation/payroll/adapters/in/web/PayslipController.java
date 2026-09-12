package gn.lacaeducation.payroll.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payroll.application.service.PayrollService;
import gn.lacaeducation.payroll.application.service.PayrollService.GenerateCommand;
import gn.lacaeducation.payroll.application.service.PayrollService.TeacherWage;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipLine;
import gn.lacaeducation.payroll.domain.PayslipLineType;
import gn.lacaeducation.payroll.domain.PayslipStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Payroll — Payslips", description = "Fiches de paie : génération, calcul, validation, paiement")
@RestController
@RequestMapping("/api/v1/payroll/payslips")
@PreAuthorize("hasAuthority('paie:manage')")
@RequiredArgsConstructor
public class PayslipController {

    private final PayrollService service;

    public record GenerateRequest(gn.lacaeducation.payroll.domain.EmployeeType employeeType,
                                  Long teacherId, String teacherName, String periodLabel, LocalDate periodStart,
                                  LocalDate periodEnd, BigDecimal baseSalary, Long structureId, String notes,
                                  Integer absentDays, Integer workingDays,
                                  BigDecimal overtimeHours, BigDecimal overtimeRate, BigDecimal overtimeMultiplier,
                                  String teacherPhone, String bankAccount) {}
    public record BatchRequest(String periodLabel, LocalDate periodStart, LocalDate periodEnd, Long structureId,
                               List<TeacherWage> wages) {}
    public record CombinedRequest(Long employeeId, String employeeName,
                                  gn.lacaeducation.payroll.domain.EmployeeType employeeType,
                                  BigDecimal baseSalary, Long linkedTeacherId, Long structureId,
                                  String employeePhone, String bankAccount) {}
    public record LineRequest(String label, PayslipLineType lineType, BigDecimal amount) {}
    public record PayRequest(String paymentMethod) {}

    @Operation(summary = "Lister les fiches (filtres enseignant / statut)")
    @GetMapping
    public List<Payslip> list(@RequestParam(required = false) Long teacherId,
                              @RequestParam(required = false) PayslipStatus status) {
        return service.find(TenantContext.get(), teacherId, status);
    }

    @Operation(summary = "Détail d'une fiche")
    @GetMapping("/{id}")
    public Payslip findById(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Lignes d'une fiche")
    @GetMapping("/{id}/lines")
    public List<PayslipLine> lines(@PathVariable Long id) { return service.lines(id); }

    @Operation(summary = "Générer une fiche individuelle (calcule base → brut → net selon la structure)")
    @PostMapping
    public Payslip generate(@RequestBody GenerateRequest r) {
        return service.generate(new GenerateCommand(TenantContext.get(), r.employeeType(),
                r.teacherId(), r.teacherName(), r.periodLabel(),
                r.periodStart(), r.periodEnd(), r.baseSalary(), r.structureId(), r.notes(),
                r.absentDays(), r.workingDays(), r.overtimeHours(), r.overtimeRate(), r.overtimeMultiplier(),
                r.teacherPhone(), r.bankAccount()));
    }

    @Operation(summary = "Générer un lot de fiches (une par enseignant/base fournis)")
    @PostMapping("/batch")
    public List<Payslip> batch(@RequestBody BatchRequest r) {
        return service.generateBatch(TenantContext.get(), r.periodLabel(), r.periodStart(), r.periodEnd(),
                r.structureId(), r.wages());
    }

    @Operation(summary = "Générer la fiche de paie combinée d'un personnel direction/administratif qui "
            + "enseigne aussi : salaire de base fixe + ligne d'heures d'enseignement calculée depuis "
            + "l'enseignant lié à sa fiche (§payroll)")
    @PostMapping("/combined")
    public Payslip generateCombined(@RequestParam int year, @RequestParam int month, @RequestBody CombinedRequest r) {
        return service.generateCombinedPayslip(TenantContext.get(), r.employeeId(), r.employeeName(),
                r.employeeType(), r.baseSalary(), r.linkedTeacherId(), year, month, r.structureId(),
                r.employeePhone(), r.bankAccount());
    }

    @Operation(summary = "Ajouter une ligne (prime/retenue) et recalculer")
    @PostMapping("/{id}/lines")
    public Payslip addLine(@PathVariable Long id, @RequestBody LineRequest r) {
        return service.addLine(id, r.label(), r.lineType(), r.amount());
    }

    @Operation(summary = "Retirer une ligne et recalculer")
    @DeleteMapping("/{id}/lines/{lineId}")
    public Payslip removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        return service.removeLine(id, lineId);
    }

    @Operation(summary = "Valider (approuver) la fiche")
    @PatchMapping("/{id}/approve")
    public Payslip approve(@PathVariable Long id) { return service.approve(id); }

    @Operation(summary = "Marquer la fiche comme payée (virement par défaut)")
    @PatchMapping("/{id}/pay")
    public Payslip pay(@PathVariable Long id, @RequestBody(required = false) PayRequest r) {
        return service.markPaid(id, r != null ? r.paymentMethod() : null);
    }

    @Operation(summary = "Supprimer une fiche en brouillon")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Télécharger la fiche de paie en PDF (A5 stylé, inline)")
    @GetMapping("/{id}/pdf/download")
    public ResponseEntity<byte[]> pdfDownload(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"fiche-paie-" + id + ".pdf\"")
                .body(service.pdfBytes(id));
    }

    @Operation(summary = "Exporter le fichier bancaire (CSV des nets à virer)")
    @GetMapping("/bank-file")
    public ResponseEntity<byte[]> bankFile() {
        byte[] csv = service.bankFileCsv(TenantContext.get()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"virements-paie.csv\"")
                .body(csv);
    }
}
