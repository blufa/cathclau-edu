package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.InstallmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Traitement des échéances en retard pour UN tenant, dans SA PROPRE transaction — extrait de
 * {@link DeadlineCheckScheduler} (plutôt qu'une méthode {@code @Transactional} appelée en interne, ce
 * qui n'aurait aucun effet : un appel {@code this.xxx()} ne passe pas par le proxy Spring qui applique
 * {@code @Transactional}).
 *
 * <p>Ne marque QUE le statut {@code OVERDUE} de l'échéance — ne déclenche plus aucun SMS automatique
 * (§retour censeur : "si on relance c'est en ce moment les sms vont, si on ne relance pas les sms ne
 * doivent pas partir"). Avant ce correctif, un {@code EcheanceDepasseeEvent} était republié CHAQUE
 * JOUR pour chaque facture encore impayée tant qu'elle restait en retard — un parent qui n'avait
 * toujours pas payé recevait donc le même SMS de relance indéfiniment, sans qu'aucun humain ne l'ait
 * demandé. La relance reste possible, mais uniquement sur déclenchement manuel d'un admin via l'écran
 * de diffusion ciblée (/communications, audience "Payeurs en retard" — {@code BroadcastAudience.
 * OVERDUE_PAYERS}), qui existait déjà en parallèle de cet envoi automatique.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class OverdueInstallmentProcessor {

    private final InstallmentRepository installmentRepository;

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
            } catch (Exception e) {
                log.error("Failed to process overdue installment id={}: {}", installment.getId(), e.getMessage());
            }
        }
    }
}
