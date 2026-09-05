package gn.lacaeducation.courses.application.service;

import gn.lacaeducation.billing.application.event.InvoicePaidEvent;
import gn.lacaeducation.courses.application.port.out.CoursePurchaseRepository;
import gn.lacaeducation.courses.domain.CoursePurchaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Débloque l'accès à un cours dès que sa facture d'achat (frais miroir OTHER) est intégralement
 * soldée (FacturePayee, §18) — quel que soit le canal de paiement utilisé (Djomy/Orange/MTN/Wave/
 * carte ou guichet), puisqu'il s'agit d'une facture normale du module billing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoursePurchasePaymentListener {

    private final CoursePurchaseRepository purchaseRepository;

    @EventListener
    @Transactional
    public void onInvoicePaid(InvoicePaidEvent event) {
        purchaseRepository.findByInvoiceId(event.invoiceId()).ifPresent(purchase -> {
            if (purchase.getStatus() == CoursePurchaseStatus.PAID) return;
            purchase.setStatus(CoursePurchaseStatus.PAID);
            purchase.setPurchasedAt(LocalDateTime.now());
            purchaseRepository.save(purchase);
            log.info("Cours id={} débloqué pour l'élève id={} (facture id={} soldée)",
                    purchase.getCourseId(), purchase.getStudentId(), event.invoiceId());
        });
    }
}
