package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.event.EcheanceDepasseeEvent;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.InstallmentStatus;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Traitement des échéances en retard pour UN tenant, dans SA PROPRE transaction — extrait de
 * {@link DeadlineCheckScheduler} (plutôt qu'une méthode {@code @Transactional} appelée en interne, ce
 * qui n'aurait aucun effet : un appel {@code this.xxx()} ne passe pas par le proxy Spring qui applique
 * {@code @Transactional}). Séparer par tenant, chacun dans sa propre transaction qui commite avant de
 * passer au tenant suivant, est aussi ce qui garantit que {@link EcheanceDepasseeEvent} est traité par
 * {@code communication} (écouteurs {@code @TransactionalEventListener}, déclenchés après commit) alors
 * que {@link gn.lacaeducation.common.TenantContext} est encore positionné sur le BON tenant — une seule
 * transaction englobant tous les tenants aurait fait tourner tous les écouteurs après la boucle
 * entière, avec un contexte tenant déjà effacé ou pointant sur le dernier tenant traité.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class OverdueInstallmentProcessor {

    private final InstallmentRepository installmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void checkOverdueDeadlinesForTenant(Long tenantId, LocalDate today) {
        List<Installment> overdue = installmentRepository.findOverdue(today);

        log.info("Overdue deadline check: found {} overdue installments for tenant {}", overdue.size(), tenantId);

        for (Installment installment : overdue) {
            try {
                if (InstallmentStatus.OVERDUE != installment.getStatus()) {
                    installment.setStatus(InstallmentStatus.OVERDUE);
                    installmentRepository.save(installment);
                }

                List<Invoice> unpaid = invoiceRepository.findByInstallmentId(installment.getId()).stream()
                        .filter(i -> i.getStatus() != InvoiceStatus.PAID)
                        .toList();
                for (Invoice invoice : unpaid) {
                    eventPublisher.publishEvent(new EcheanceDepasseeEvent(
                            tenantId,
                            invoice.getStudentId(),
                            installment.getId(),
                            installment.getFeeId(),
                            invoice.getReference(),
                            installment.getAmount(),
                            installment.getDueDate()
                    ));
                }

                log.debug("Emitted EcheanceDepasseeEvent for installment id={} fee id={} ({} élève(s) impayé(s))",
                        installment.getId(), installment.getFeeId(), unpaid.size());
            } catch (Exception e) {
                log.error("Failed to process overdue installment id={}: {}", installment.getId(), e.getMessage());
            }
        }
    }
}
