package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.platform.application.event.SubscriptionActivatedEvent;
import gn.lacaeducation.platform.application.port.in.InvoiceUseCase;
import gn.lacaeducation.platform.application.port.in.PaymentUseCase;
import gn.lacaeducation.platform.application.port.in.PlanChangeUseCase;
import gn.lacaeducation.platform.application.port.in.SubscriptionUseCase;
import gn.lacaeducation.platform.application.port.in.VerifyStudentQuotaUseCase;
import gn.lacaeducation.platform.application.port.out.PlanChangeRepository;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionInvoiceRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionPaymentRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionReceiptRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.domain.BillingPeriod;
import gn.lacaeducation.platform.domain.InvoiceStatus;
import gn.lacaeducation.platform.domain.PaymentStatus;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanChange;
import gn.lacaeducation.platform.domain.PlanChangeType;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.SubscriptionInvoice;
import gn.lacaeducation.platform.domain.SubscriptionPayment;
import gn.lacaeducation.platform.domain.SubscriptionReceipt;
import gn.lacaeducation.platform.domain.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AbonnementService implements SubscriptionUseCase, InvoiceUseCase,
        PaymentUseCase, PlanChangeUseCase, VerifyStudentQuotaUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionReceiptRepository receiptRepository;
    private final PlanChangeRepository planChangeRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ---- SubscriptionUseCase ----

    @Override
    public Subscription initiate(InitiateSubscriptionCommand command) {
        Plan plan = planRepository.findById(command.planId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + command.planId()));

        BigDecimal amount = command.billingPeriod() == BillingPeriod.ANNUAL
                ? plan.getAnnualPrice() : plan.getMonthlyPrice();

        LocalDate start = LocalDate.now();
        LocalDate end = command.billingPeriod() == BillingPeriod.ANNUAL
                ? start.plusYears(1) : start.plusMonths(1);

        Subscription subscription = Subscription.builder()
                .tenantId(command.tenantId())
                .planId(command.planId())
                .startDate(start)
                .endDate(end)
                .status(SubscriptionStatus.PENDING)
                .autoRenewal(command.autoRenewal())
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        generate(saved.getId(), command.billingPeriod(), start, end);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Subscription findById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findActive(Long tenantId) {
        return subscriptionRepository.findActiveByTenantId(tenantId);
    }

    @Override @Transactional(readOnly = true)
    public List<Subscription> findAll() { return subscriptionRepository.findAll(); }

    @Override @Transactional(readOnly = true)
    public Optional<Subscription> findActiveForCurrentTenant() {
        Long tenantId = gn.lacaeducation.iam.TenantContext.get();
        if (tenantId == null) return Optional.empty();
        return subscriptionRepository.findActiveByTenantId(tenantId);
    }

    @Override
    public void suspend(Long subscriptionId) {
        Subscription s = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        s.setStatus(SubscriptionStatus.SUSPENDED);
        subscriptionRepository.save(s);
    }

    @Override
    public void cancel(Long subscriptionId) {
        Subscription s = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        s.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(s);
    }

    @Override
    public void setAutoRenewal(Long subscriptionId, boolean enabled) {
        Subscription s = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        s.setAutoRenewal(enabled);
        subscriptionRepository.save(s);
    }

    // ---- InvoiceUseCase ----

    @Override
    public SubscriptionInvoice generate(Long subscriptionId, BillingPeriod period, LocalDate start, LocalDate end) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        BigDecimal net = subscription.getAmount();
        BigDecimal tax = net.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = net.add(tax);

        SubscriptionInvoice invoice = SubscriptionInvoice.builder()
                .subscriptionId(subscriptionId)
                .reference(generateRef("INV"))
                .billingPeriod(period)
                .periodStart(start)
                .periodEnd(end)
                .netAmount(net)
                .taxAmount(tax)
                .totalAmount(total)
                .paidAmount(BigDecimal.ZERO)
                .remainingBalance(total)
                .status(InvoiceStatus.ISSUED)
                .issuedAt(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionInvoice findInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionInvoice findLatestBySubscription(Long subscriptionId) {
        return invoiceRepository.findLatestBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("No invoice for subscription: " + subscriptionId));
    }

    // ---- PaymentUseCase ----

    @Override
    public SubscriptionPayment initiate(InitiatePaymentCommand command) {
        invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + command.invoiceId()));

        SubscriptionPayment payment = SubscriptionPayment.builder()
                .invoiceId(command.invoiceId())
                .reference(generateRef("PAY"))
                .amount(command.amount())
                .channel(command.channel())
                .paymentType(command.paymentType())
                .status(PaymentStatus.INITIATED)
                .paidAt(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }

    @Override
    public SubscriptionPayment confirm(Long paymentId, String externalId) {
        SubscriptionPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        payment.setStatus(PaymentStatus.SUCCESSFUL);
        payment.setExternalTransactionId(externalId);
        SubscriptionPayment saved = paymentRepository.save(payment);

        processSuccessfulPayment(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPayment findPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
    }

    private void processSuccessfulPayment(SubscriptionPayment payment) {
        SubscriptionInvoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        BigDecimal newPaid = invoice.getPaidAmount().add(payment.getAmount());
        BigDecimal newBalance = invoice.getTotalAmount().subtract(newPaid).max(BigDecimal.ZERO);
        invoice.setPaidAmount(newPaid);
        invoice.setRemainingBalance(newBalance);
        invoice.setStatus(invoice.isPaid() ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL);
        invoiceRepository.save(invoice);

        if (invoice.isPaid()) {
            subscriptionRepository.findById(invoice.getSubscriptionId()).ifPresent(subscription -> {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscriptionRepository.save(subscription);

                SubscriptionReceipt receipt = SubscriptionReceipt.builder()
                        .paymentId(payment.getId())
                        .reference(generateRef("REC"))
                        .amount(payment.getAmount())
                        .generatedAt(LocalDateTime.now())
                        .build();
                receiptRepository.save(receipt);

                eventPublisher.publishEvent(new SubscriptionActivatedEvent(subscription.getTenantId(), subscription.getId()));
            });
        }
    }

    // ---- PlanChangeUseCase ----

    @Override
    public void changePlan(ChangePlanCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + command.subscriptionId()));
        Plan oldPlan = planRepository.findById(subscription.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + subscription.getPlanId()));
        Plan newPlan = planRepository.findById(command.newPlanId())
                .orElseThrow(() -> new IllegalArgumentException("New plan not found: " + command.newPlanId()));

        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), subscription.getEndDate());
        BigDecimal oldPrice = command.newBillingPeriod() == BillingPeriod.ANNUAL
                ? oldPlan.getAnnualPrice() : oldPlan.getMonthlyPrice();
        BigDecimal newPrice = command.newBillingPeriod() == BillingPeriod.ANNUAL
                ? newPlan.getAnnualPrice() : newPlan.getMonthlyPrice();
        BigDecimal prorated = BigDecimal.valueOf(daysLeft)
                .multiply(newPrice.subtract(oldPrice))
                .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);

        PlanChangeType type = newPrice.compareTo(oldPrice) > 0 ? PlanChangeType.UPGRADE : PlanChangeType.DOWNGRADE;

        PlanChange change = PlanChange.builder()
                .subscriptionId(command.subscriptionId())
                .oldPlanId(subscription.getPlanId())
                .newPlanId(command.newPlanId())
                .type(type)
                .proratedAmount(prorated)
                .effectiveDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build();
        planChangeRepository.save(change);

        subscription.setPlanId(command.newPlanId());
        subscription.setAmount(newPrice);
        subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanChange> findBySubscription(Long subscriptionId) {
        return planChangeRepository.findBySubscriptionId(subscriptionId);
    }

    // ---- VerifyStudentQuotaUseCase ----

    @Override
    @Transactional(readOnly = true)
    public boolean isQuotaAvailable(Long tenantId, int currentStudentCount) {
        return subscriptionRepository.findActiveByTenantId(tenantId)
                .flatMap(s -> planRepository.findById(s.getPlanId()))
                .map(plan -> plan.getStudentLimit() == null || currentStudentCount < plan.getStudentLimit())
                .orElse(false);
    }

    private String generateRef(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + "-" + date + "-" + suffix;
    }
}
