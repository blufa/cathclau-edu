package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.event.InvoiceOverpaidEvent;
import gn.lacaeducation.billing.application.port.in.StudentCreditUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pose le trop-perçu (§4 cahier des charges) comme crédit disponible de l'élève — découplé de
 * {@code InvoiceService#applyPayment} par événement (comme {@code InvoicePaymentListener} et
 * {@code ClassTransferInvoiceListener}) pour éviter une dépendance circulaire
 * InvoiceService → StudentCreditUseCase → InvoiceUseCase (StudentCreditService appelle déjà
 * {@code invoiceUseCase.applyCredit} pour réappliquer le crédit à une facture future).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentCreditFromOverpaymentListener {

    private final StudentCreditUseCase studentCreditUseCase;

    @EventListener
    @Transactional
    public void onInvoiceOverpaid(InvoiceOverpaidEvent event) {
        studentCreditUseCase.create(event.tenantId(), event.studentId(), event.excessAmount(),
                event.paymentId(), event.invoiceId(), "Trop-perçu sur facture " + event.invoiceId());
        log.info("Trop-perçu de {} posé en crédit disponible pour l'élève id={} (facture id={})",
                event.excessAmount(), event.studentId(), event.invoiceId());
    }
}
