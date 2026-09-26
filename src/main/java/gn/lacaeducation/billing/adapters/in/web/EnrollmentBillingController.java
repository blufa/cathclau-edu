package gn.lacaeducation.billing.adapters.in.web;

import gn.lacaeducation.billing.application.port.in.EnrollmentBillingUseCase;
import gn.lacaeducation.billing.application.port.in.EnrollmentBillingUseCase.GeneratedFeeInvoices;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.EnrollmentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Génération groupée de toutes les factures de classe à l'inscription (§retour comptable, refonte
 * inscription §5.2) — voir {@link EnrollmentBillingUseCase}. */
@Tag(name = "Enrollment Billing", description = "Génération groupée des factures de classe à l'inscription")
@RestController
@RequestMapping("/api/v1/billing/enrollment-billing")
@RequiredArgsConstructor
public class EnrollmentBillingController {

    private final EnrollmentBillingUseCase useCase;

    public record GenerateAllRequest(Long studentId, Long classId, Long academicYearId,
                                     EnrollmentType enrollmentType, boolean wantsTransport, Long transportFeeId) {}

    @Operation(summary = "Générer toutes les factures des frais rattachés à la classe d'un élève (+ transport si choisi)")
    @PreAuthorize("hasAuthority('inscription:create')")
    @PostMapping("/generate-all")
    public List<GeneratedFeeInvoices> generateAll(@RequestBody GenerateAllRequest r) {
        return useCase.generateAllForEnrollment(new EnrollmentBillingUseCase.GenerateAllFeesCommand(
                TenantContext.get(), r.studentId(), r.classId(), r.academicYearId(),
                r.enrollmentType(), r.wantsTransport(), r.transportFeeId()));
    }
}
