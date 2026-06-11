package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.event.EcheanceDepasseeEvent;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.InstallmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily scheduled job that detects overdue installments.
 * For each installment whose due_date has passed and that is not yet PAID,
 * it marks it OVERDUE and emits an EcheanceDepasseeEvent so the communication
 * module can send reminders to parents.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineCheckScheduler {

    private final InstallmentRepository installmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 6 * * *") // daily at 6am
    @Transactional
    public void checkOverdueDeadlines() {
        LocalDate today = LocalDate.now();
        List<Installment> overdue = installmentRepository.findOverdue(today);

        log.info("Overdue deadline check: found {} overdue installments", overdue.size());

        for (Installment installment : overdue) {
            try {
                // Mark as OVERDUE if not already
                if (InstallmentStatus.OVERDUE != installment.getStatus()) {
                    installment.setStatus(InstallmentStatus.OVERDUE);
                    installmentRepository.save(installment);
                }

                eventPublisher.publishEvent(new EcheanceDepasseeEvent(
                        installment.getId(),
                        installment.getFeeId(),
                        installment.getAmount(),
                        installment.getDueDate()
                ));

                log.debug("Emitted EcheanceDepasseeEvent for installment id={} fee id={}",
                        installment.getId(), installment.getFeeId());
            } catch (Exception e) {
                log.error("Failed to process overdue installment id={}: {}", installment.getId(), e.getMessage());
            }
        }
    }
}
