package gn.lacaeducation.payroll.adapters.in.web;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payroll.application.service.PayrollService;
import gn.lacaeducation.payroll.application.service.PayrollService.GenerateCommand;
import gn.lacaeducation.payroll.application.service.PayrollService.TeacherWage;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipLine;
import gn.lacaeducation.payroll.domain.PayslipLineType;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.PayslipStatus;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final TeacherPublicInfoProvider teacherPublicInfoProvider;
    private final EmployeePublicInfoProvider employeePublicInfoProvider;

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
    public Payslip pay(@PathVariable Long id, @RequestBody(required = false) PayRequest r,
                       @AuthenticationPrincipal Jwt jwt) {
        return service.markPaid(id, r != null ? r.paymentMethod() : null, currentUserId(jwt));
    }

    /** Comptable connecté ayant marqué la fiche payée (imprimé sur le PDF) — extrait du JWT (sub),
     * même principe que {@code PaymentController#currentUserId}. */
    private static Long currentUserId(Jwt jwt) {
        try {
            return jwt == null ? null : Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Operation(summary = "Supprimer une fiche en brouillon")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Télécharger la fiche de paie en PDF (A4 stylé, couleur d'école personnalisable, inline)")
    @GetMapping("/{id}/pdf/download")
    public ResponseEntity<byte[]> pdfDownload(@PathVariable Long id) {
        Payslip p = service.findById(id);
        return pdfResponse(id, p);
    }

    // ── Self-service enseignant (§Vision enseignant) ────────────────────────
    // Ces trois endpoints court-circuitent le `@PreAuthorize("hasAuthority('paie:manage')")` de
    // classe (une annotation de méthode a priorité sur celle de classe) : un enseignant n'a jamais
    // cette permission, mais doit pouvoir consulter SES PROPRES fiches — jamais celles d'un collègue,
    // d'où la vérification explicite teacherId == fiche du salarié connecté sur chaque accès.

    @Operation(summary = "Mes fiches de paie (self-service enseignant)")
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/mine")
    public List<Payslip> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.find(TenantContext.get(), myTeacherId(jwt), null);
    }

    @Operation(summary = "Détail d'une de mes fiches (self-service enseignant)")
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/mine/{id}")
    public Payslip mineById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return requireOwn(jwt, id);
    }

    @Operation(summary = "Télécharger le PDF d'une de mes fiches (self-service enseignant)")
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/mine/{id}/pdf/download")
    public ResponseEntity<byte[]> minePdfDownload(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Payslip p = requireOwn(jwt, id);
        return pdfResponse(id, p);
    }

    private ResponseEntity<byte[]> pdfResponse(Long id, Payslip p) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"fiche-paie-" + id + ".pdf\"")
                .body(service.pdfBytes(id, beneficiaryRole(p), beneficiaryPhotoUrl(p)));
    }

    /** Sous-titre affiché sous le nom du salarié sur le PDF (ex. "Mathématiques", "Principal") — résolu
     * ici (pas dans {@code PayrollService}, volontairement découplée du module staff) ; le suffixe
     * "payé à l'heure"/"salaire fixe" est ajouté par le générateur PDF lui-même (déjà au courant via
     * {@code TeacherHours}, pas besoin de le dupliquer ici). */
    private String beneficiaryRole(Payslip p) {
        if (p.getTeacherId() == null) return null;
        if (p.getEmployeeType() == null || p.getEmployeeType() == EmployeeType.TEACHER) {
            return teacherPublicInfoProvider.findTeacher(p.getTeacherId())
                    .map(TeacherPublicInfoProvider.TeacherInfo::specialization).orElse(null);
        }
        return employeePublicInfoProvider.findEmployee(p.getTeacherId())
                .map(EmployeePublicInfoProvider.EmployeeInfo::jobTitle).orElse(null);
    }

    private String beneficiaryPhotoUrl(Payslip p) {
        if (p.getTeacherId() == null) return null;
        if (p.getEmployeeType() == null || p.getEmployeeType() == EmployeeType.TEACHER) {
            return teacherPublicInfoProvider.findTeacher(p.getTeacherId())
                    .map(TeacherPublicInfoProvider.TeacherInfo::photo).orElse(null);
        }
        return employeePublicInfoProvider.findEmployee(p.getTeacherId())
                .map(EmployeePublicInfoProvider.EmployeeInfo::photo).orElse(null);
    }

    private Payslip requireOwn(Jwt jwt, Long payslipId) {
        Payslip p = service.findById(payslipId);
        if (!p.getTeacherId().equals(myTeacherId(jwt))) {
            // 403 (autorisation), pas 422 (règle métier) : la ressource existe, elle n'appartient
            // simplement pas à l'appelant — jamais confondre les deux (voir CLAUDE.md §codes HTTP).
            throw new org.springframework.security.access.AccessDeniedException("Cette fiche de paie ne vous appartient pas");
        }
        return p;
    }

    private Long myTeacherId(Jwt jwt) {
        Long userId;
        try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
        return teacherPublicInfoProvider.findTeacherIdByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException("Aucune fiche enseignant liée à votre compte"));
    }

}
