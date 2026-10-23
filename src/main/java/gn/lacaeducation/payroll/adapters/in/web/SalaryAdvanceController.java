package gn.lacaeducation.payroll.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payroll.application.port.in.SalaryAdvanceUseCase;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.SalaryAdvance;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Payroll — Avances sur salaire", description = "Octroi plafonné, déduction automatique sur la paie, bon PDF")
@RestController
@RequestMapping("/api/v1/payroll/salary-advances")
@PreAuthorize("hasAuthority('paie:manage')")
@RequiredArgsConstructor
public class SalaryAdvanceController {

    private final SalaryAdvanceUseCase service;

    public record GrantRequest(EmployeeType employeeType, Long teacherId, String teacherName,
                               BigDecimal amount, String reason) {}

    @Operation(summary = "Lister les avances sur salaire (toutes, ou celles d'un salarié précis)")
    @GetMapping
    public List<SalaryAdvance> list(@RequestParam(required = false) Long teacherId,
                                    @RequestParam(required = false) EmployeeType employeeType) {
        if (teacherId != null && employeeType != null) {
            return service.findByTeacherId(TenantContext.get(), teacherId, employeeType);
        }
        return service.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Accorder une avance sur salaire (plafonnée au salaire de référence du salarié)")
    @PostMapping
    public SalaryAdvance grant(@RequestBody GrantRequest r, @AuthenticationPrincipal Jwt jwt) {
        return service.grant(new SalaryAdvanceUseCase.GrantCommand(TenantContext.get(), r.employeeType(),
                r.teacherId(), r.teacherName(), r.amount(), r.reason(), currentUserId(jwt)));
    }

    /** Comptable connecté ayant accordé l'avance (imprimé sur le bon) — extrait du JWT (sub), même
     * principe que {@code PaymentController#currentUserId}. */
    private static Long currentUserId(Jwt jwt) {
        try {
            return jwt == null ? null : Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Operation(summary = "Annuler une avance non encore déduite")
    @PatchMapping("/{id}/annuler")
    public SalaryAdvance cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @Operation(summary = "Télécharger le bon d'avance sur salaire en PDF")
    @GetMapping("/{id}/pdf/download")
    public ResponseEntity<byte[]> pdfDownload(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"avance-salaire-" + id + ".pdf\"")
                .body(service.voucherPdf(id));
    }
}
