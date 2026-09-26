package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.EnrollmentBillingUseCase;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.common.SchoolCode;
import gn.lacaeducation.common.TenantInfoProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Génération groupée des factures de classe à l'inscription (§retour comptable, refonte inscription
 * §5.2) — voir {@link EnrollmentBillingUseCase}. Aucun calcul de remise ici : délégué entièrement à
 * {@link InvoiceUseCase}, qui l'applique déjà de façon transparente.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EnrollmentBillingService implements EnrollmentBillingUseCase {

    private final SchoolFeeRepository schoolFeeRepository;
    private final InstallmentRepository installmentRepository;
    private final InvoiceUseCase invoiceUseCase;
    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public List<GeneratedFeeInvoices> generateAllForEnrollment(GenerateAllFeesCommand cmd) {
        // Le type opposé (ENROLLMENT vs RE_ENROLLMENT) est exclu si la classe a les deux frais
        // configurés — une nouvelle inscription ne doit jamais générer de facture de réinscription,
        // et inversement (§retour comptable).
        FeeType excludedType = cmd.enrollmentType() == gn.lacaeducation.students.EnrollmentType.RE_ENROLLMENT
                ? FeeType.ENROLLMENT : FeeType.RE_ENROLLMENT;
        List<SchoolFee> classFees = schoolFeeRepository.findByClassId(cmd.classId()).stream()
                .filter(fee -> fee.getType() != excludedType)
                .toList();

        List<GeneratedFeeInvoices> result = new ArrayList<>();
        for (SchoolFee fee : classFees) {
            result.add(generateForFee(cmd.tenantId(), cmd.studentId(), fee, cmd.academicYearId()));
        }

        // Frais TRANSPORT (§billing) : rattaché à une ligne de bus (routeId), jamais à la classe —
        // ne peut donc jamais être résolu automatiquement via findByClassId ci-dessus.
        if (cmd.wantsTransport() && cmd.transportFeeId() != null) {
            schoolFeeRepository.findById(cmd.transportFeeId())
                    .filter(fee -> fee.getType() == FeeType.TRANSPORT)
                    .ifPresentOrElse(
                            fee -> result.add(generateForFee(cmd.tenantId(), cmd.studentId(), fee, cmd.academicYearId())),
                            () -> log.warn("Frais transport id={} introuvable ou de type incorrect ; ignoré pour l'élève id={}",
                                    cmd.transportFeeId(), cmd.studentId()));
        }
        return result;
    }

    private GeneratedFeeInvoices generateForFee(Long tenantId, Long studentId, SchoolFee fee, Long academicYearId) {
        boolean hasInstallments = !installmentRepository.findByFeeId(fee.getId()).isEmpty();
        List<Invoice> invoices;
        if (hasInstallments) {
            // generateMonthly() est déjà idempotent par échéance et applique déjà les remises.
            invoices = invoiceUseCase.generateMonthly(
                    new InvoiceUseCase.GenerateMonthlyCommand(tenantId, studentId, fee.getId(), academicYearId));
        } else if (!invoiceUseCase.existsByStudentIdAndFeeId(studentId, fee.getId())) {
            String reference = generateReference(tenantId);
            invoices = List.of(invoiceUseCase.create(new InvoiceUseCase.CreateInvoiceCommand(
                    tenantId, studentId, fee.getId(), reference, fee.getAmount(), academicYearId)));
        } else {
            invoices = List.of();
        }
        log.info("Génération groupée inscription : {} facture(s) créée(s) pour l'élève id={} frais id={} ({})",
                invoices.size(), studentId, fee.getId(), fee.getType());
        return new GeneratedFeeInvoices(fee, invoices);
    }

    private String generateReference(Long tenantId) {
        String prefix = SchoolCode.prefix(tenantInfoProvider, tenantId);
        String year = String.valueOf(java.time.Year.now().getValue());
        String shortId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-FRA-" + year + "-" + shortId;
    }
}
