package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase.CreateInvoiceCommand;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.common.SchoolCode;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.students.application.event.EnrollmentCreatedEvent;
import gn.lacaeducation.students.EnrollmentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Listens to EnrollmentCreatedEvent and automatically creates an ENROLLMENT invoice as soon as a
 * student is enrolled (status PENDING), when a school fee of type ENROLLMENT exists for that class.
 * The fee can then be collected; full payment validates the enrollment (see
 * {@code EnrollmentValidationOnPaymentListener}). The tenant is derived from the matching SchoolFee.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentInvoiceListener {

    private final SchoolFeeRepository schoolFeeRepository;
    private final InvoiceUseCase invoiceUseCase;
    private final TenantInfoProvider tenantInfoProvider;

    @EventListener
    @Transactional
    public void onEnrollmentCreated(EnrollmentCreatedEvent event) {
        FeeType feeType = feeTypeFor(event.type());
        log.info("Inscription reçue : eleveId={} classeId={} inscriptionId={} type={} -> recherche du frais {} (tenant={})",
                event.studentId(), event.classId(), event.enrollmentId(), event.type(), feeType, TenantContext.get());

        // Pour une réinscription, on privilégie le frais REINSCRIPTION mais on retombe sur le frais
        // INSCRIPTION quand l'école n'a configuré qu'un seul frais d'inscription pour la classe.
        List<SchoolFee> fees = schoolFeeRepository.findByClassIdAndType(event.classId(), feeType);
        if (fees.isEmpty() && feeType == FeeType.RE_ENROLLMENT) {
            log.info("Aucun frais {} pour la classe id={}, repli sur le frais {}", feeType, event.classId(), FeeType.ENROLLMENT);
            feeType = FeeType.ENROLLMENT;
            fees = schoolFeeRepository.findByClassIdAndType(event.classId(), feeType);
        }

        if (fees.isEmpty()) {
            log.warn("Aucun frais {} configuré pour la classe id={} (tenant={}) ; AUCUNE facture d'inscription créée pour l'élève id={}. "
                            + "Configurez un frais via POST /api/v1/billing/fees pour cette classe.",
                    feeType, event.classId(), TenantContext.get(), event.studentId());
            return;
        }

        for (SchoolFee fee : fees) {
            // §13 cahier des charges : un frais d'inscription/réinscription marqué facultatif n'est
            // jamais facturé automatiquement ici (rare en pratique, mais la configuration l'autorise).
            if (!Boolean.TRUE.equals(fee.getMandatory())) {
                log.info("Frais {} id={} facultatif ; aucune facture automatique pour l'élève id={}",
                        feeType, fee.getId(), event.studentId());
                continue;
            }
            // Idempotence : on saute si une facture existe déjà pour ce couple élève + frais
            if (invoiceUseCase.existsByStudentIdAndFeeId(event.studentId(), fee.getId())) {
                log.info("Facture déjà existante pour l'élève id={} frais id={}, ignorée (idempotent)",
                        event.studentId(), fee.getId());
                continue;
            }

            String reference = generateReference(fee.getTenantId(), event.studentId(), fee.getId());

            try {
                Invoice created = invoiceUseCase.create(new CreateInvoiceCommand(
                        fee.getTenantId(),
                        event.studentId(),
                        fee.getId(),
                        reference,
                        fee.getAmount(),
                        event.academicYearId()
                ));
                // §retour comptable, fondation : élève entièrement exonéré (ex. "tous les frais") →
                // aucune facture créée par InvoiceService#create, c'est le comportement attendu.
                if (created != null) {
                    log.info("Facture d'inscription créée automatiquement réf={} pour l'élève id={} classe id={} frais id={} montant={}",
                            reference, event.studentId(), event.classId(), fee.getId(), fee.getAmount());
                } else {
                    log.info("Frais d'inscription id={} entièrement exonéré pour l'élève id={} classe id={} ; aucune facture créée.",
                            fee.getId(), event.studentId(), event.classId());
                }
            } catch (RuntimeException ex) {
                log.error("Échec de création automatique de la facture d'inscription pour l'élève id={} classe id={} frais id={} : {}",
                        event.studentId(), event.classId(), fee.getId(), ex.getMessage(), ex);
                throw ex;
            }
        }
    }

    /** Associe le type d'inscription au type de frais facturé à la création. */
    private static FeeType feeTypeFor(EnrollmentType type) {
        return type == EnrollmentType.RE_ENROLLMENT ? FeeType.RE_ENROLLMENT : FeeType.ENROLLMENT;
    }

    private String generateReference(Long tenantId, Long studentId, Long feeId) {
        // Format : <PREFIXE_ECOLE>-INS-<année>-<short-uuid>
        String prefix = SchoolCode.prefix(tenantInfoProvider, tenantId);
        String year = String.valueOf(java.time.Year.now().getValue());
        String shortId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-INS-" + year + "-" + shortId;
    }
}
