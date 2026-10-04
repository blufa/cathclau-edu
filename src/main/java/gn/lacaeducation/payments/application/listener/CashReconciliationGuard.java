package gn.lacaeducation.payments.application.listener;

import gn.lacaeducation.payments.application.event.CashCollectedWithoutOpenSessionEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.payments.application.port.out.CashierSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Détecteur B2 (§retour fondateurs, contrôle anti-fraude comptable) : un encaissement CASH guichet
 * sans session de caisse ouverte pour son collecteur n'a jamais été rapproché — c'est en soi un
 * signal suspect (pas forcément un vol, mais un trou dans le contrôle). {@code collectedByUserId} de
 * {@link PaymentSucceededEvent} n'est renseigné QUE pour un paiement CASH guichet (voir sa Javadoc) :
 * un paiement en ligne/non-CASH n'a donc jamais à être vérifié ici.
 */
@Component
@RequiredArgsConstructor
public class CashReconciliationGuard {

    private final CashierSessionRepository cashierSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        if (event.collectedByUserId() == null) return;
        if (cashierSessionRepository.findOpenByCashierId(event.collectedByUserId()).isPresent()) return;
        eventPublisher.publishEvent(new CashCollectedWithoutOpenSessionEvent(
                event.tenantId(), event.collectedByUserId(), event.paymentId(), event.amount()));
    }
}
