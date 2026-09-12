package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.billing.SchoolBillingStatsProvider;
import gn.lacaeducation.billing.application.port.out.ExemptionRepository;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.Exemption;
import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.billing.domain.SchoolFee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Agrège la facturation d'un tenant pour le reporting financier (§10.2, §Vision Fondateur). */
@Component
@RequiredArgsConstructor
class SchoolBillingStatsProviderAdapter implements SchoolBillingStatsProvider {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String FAMILY_DISCOUNT_REASON_PREFIX = "Réduction familiale : ";
    private static final BigDecimal EARLY_PAYMENT_ESTIMATE_RATE = BigDecimal.valueOf(0.05);

    private final InvoiceRepository invoiceRepository;
    private final SchoolFeeRepository schoolFeeRepository;
    private final ExemptionRepository exemptionRepository;

    @Override
    @Transactional(readOnly = true)
    public BillingStats billingStats(Long tenantId) {
        return computeStats(excludeCancelled(invoiceRepository.findByTenantId(tenantId)));
    }

    @Override
    @Transactional(readOnly = true)
    public RecoveryBreakdown recoveryBreakdown(Long tenantId, Long academicYearId, int topDebtorLimit) {
        // Une facture CANCELLED (ex. mensualités d'une ancienne classe après un transfert) ne doit
        // plus peser ni dans le facturé ni dans l'impayé — elle n'est plus due.
        List<Invoice> invoices = excludeCancelled(invoiceRepository.findByTenantIdAndAcademicYearId(tenantId, academicYearId));
        Map<Long, Long> feeIdToClassId = schoolFeeRepository.findByTenantId(tenantId).stream()
                .filter(f -> f.getClassId() != null)
                .collect(Collectors.toMap(SchoolFee::getId, SchoolFee::getClassId, (a, b) -> a));

        List<ClassRecovery> byClass = invoices.stream()
                .filter(i -> i.getFeeId() != null && feeIdToClassId.get(i.getFeeId()) != null)
                .collect(Collectors.groupingBy(i -> feeIdToClassId.get(i.getFeeId())))
                .entrySet().stream()
                .map(e -> {
                    BillingStats s = computeStats(e.getValue());
                    return new ClassRecovery(e.getKey(), s.invoiced(), s.collected(), s.outstanding(), s.recoveryRate());
                })
                .sorted(Comparator.comparing(ClassRecovery::classId))
                .toList();

        List<PeriodRecovery> byPeriod = invoices.stream()
                .filter(i -> i.getPeriodLabel() != null)
                .collect(Collectors.groupingBy(Invoice::getPeriodLabel))
                .entrySet().stream()
                .map(e -> {
                    BillingStats s = computeStats(e.getValue());
                    return new PeriodRecovery(e.getKey(), s.invoiced(), s.collected(), s.outstanding(), s.recoveryRate());
                })
                .sorted(Comparator.comparing(PeriodRecovery::periodLabel))
                .toList();

        List<TopDebtor> topDebtors = invoices.stream()
                .filter(i -> i.getRemainingBalance() != null && i.getRemainingBalance().signum() > 0)
                .collect(Collectors.groupingBy(Invoice::getStudentId,
                        Collectors.reducing(BigDecimal.ZERO, Invoice::getRemainingBalance, BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(Math.max(1, topDebtorLimit))
                .map(e -> new TopDebtor(e.getKey(), studentClassId(invoices, e.getKey(), feeIdToClassId), e.getValue()))
                .toList();

        return new RecoveryBreakdown(computeStats(invoices), byClass, byPeriod, topDebtors);
    }

    @Override
    @Transactional(readOnly = true)
    public SubsidyStats subsidyStats(Long tenantId, Long academicYearId) {
        List<Exemption> exemptions = exemptionRepository.findByTenantId(tenantId).stream()
                .filter(Exemption::isActive)
                .toList();
        long totalExemptions = exemptions.stream().filter(e -> e.getType() == ExemptionType.TOTAL).count();
        long partialExemptions = exemptions.stream().filter(e -> e.getType() == ExemptionType.PARTIAL).count();

        List<Invoice> yearInvoices = excludeCancelled(invoiceRepository.findByTenantIdAndAcademicYearId(tenantId, academicYearId));

        BigDecimal familyDiscountAmount = yearInvoices.stream()
                .filter(i -> i.getDiscountReason() != null && i.getDiscountReason().startsWith(FAMILY_DISCOUNT_REASON_PREFIX))
                .map(Invoice::getDiscountAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, FeeType> feeIdToType = schoolFeeRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(SchoolFee::getId, SchoolFee::getType, (a, b) -> a));

        Map<Long, List<Invoice>> monthlyByStudent = yearInvoices.stream()
                .filter(i -> i.getFeeId() != null && feeIdToType.get(i.getFeeId()) == FeeType.MONTHLY)
                .collect(Collectors.groupingBy(Invoice::getStudentId));

        List<Map.Entry<Long, List<Invoice>>> fullySettled = monthlyByStudent.entrySet().stream()
                .filter(e -> e.getValue().stream().allMatch(i -> i.getStatus() == InvoiceStatus.PAID))
                .toList();

        BigDecimal fullySettledMonthlyTotal = fullySettled.stream()
                .flatMap(e -> e.getValue().stream())
                .map(Invoice::getTotalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal estimatedEarlyPaymentSavings = fullySettledMonthlyTotal
                .multiply(EARLY_PAYMENT_ESTIMATE_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        return new SubsidyStats(exemptions.size(), totalExemptions, partialExemptions, familyDiscountAmount,
                fullySettled.size(), estimatedEarlyPaymentSavings);
    }

    private Long studentClassId(List<Invoice> invoices, Long studentId, Map<Long, Long> feeIdToClassId) {
        return invoices.stream()
                .filter(i -> studentId.equals(i.getStudentId()) && i.getFeeId() != null)
                .map(i -> feeIdToClassId.get(i.getFeeId()))
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
    }

    private BillingStats computeStats(List<Invoice> invoices) {
        BigDecimal invoiced = sum(invoices, Invoice::getTotalAmount);
        BigDecimal collected = sum(invoices, Invoice::getPaidAmount);
        BigDecimal outstanding = sum(invoices, Invoice::getRemainingBalance);
        BigDecimal recoveryRate = invoiced.signum() == 0 ? BigDecimal.ZERO
                : collected.multiply(HUNDRED).divide(invoiced, 2, RoundingMode.HALF_UP);
        long unpaidCount = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.PARTIAL)
                .count();
        return new BillingStats(invoiced, collected, outstanding, recoveryRate, unpaidCount);
    }

    private List<Invoice> excludeCancelled(List<Invoice> invoices) {
        return invoices.stream().filter(i -> i.getStatus() != InvoiceStatus.CANCELLED).toList();
    }

    private BigDecimal sum(List<Invoice> invoices, Function<Invoice, BigDecimal> field) {
        return invoices.stream()
                .map(field)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
