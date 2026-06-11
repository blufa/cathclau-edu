package gn.lacaeducation.platform.application.port.in;

import gn.lacaeducation.platform.domain.BillingPeriod;
import gn.lacaeducation.platform.domain.SubscriptionInvoice;

import java.time.LocalDate;

public interface InvoiceUseCase {
    SubscriptionInvoice generate(Long subscriptionId, BillingPeriod period, LocalDate start, LocalDate end);
    SubscriptionInvoice findInvoiceById(Long id);
    SubscriptionInvoice findLatestBySubscription(Long subscriptionId);
}
