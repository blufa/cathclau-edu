package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.billing.SchoolBillingStatsProvider;
import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort;
import gn.lacaeducation.billing.application.port.out.ExemptionRepository;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.Exemption;
import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.billing.domain.SchoolFee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    // Préfixe exact posé par InvoiceService#applyEarlyPaymentDiscount — sert à isoler cette remise
    // précise parmi toutes les remises possibles sur une facture (discountReason est un texte libre).
    private static final String EARLY_PAYMENT_DISCOUNT_REASON_PREFIX = "Remise paiement anticipé : ";

    private final InvoiceRepository invoiceRepository;
    private final SchoolFeeRepository schoolFeeRepository;
    private final ExemptionRepository exemptionRepository;
    private final InstallmentRepository installmentRepository;
    private final EnrollmentQueryPort enrollmentQueryPort;

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

        // Remises réellement déduites via une exonération/avantage personnel (discountSource non nul) —
        // seul mécanisme de remise manuel effectivement utilisé aujourd'hui, voir doc du champ.
        List<Invoice> exemptionDiscountedInvoices = yearInvoices.stream()
                .filter(i -> i.getDiscountSource() != null)
                .toList();
        BigDecimal exemptionDiscountAmount = exemptionDiscountedInvoices.stream()
                .map(Invoice::getDiscountAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long exemptionDiscountInvoicePaidCount = exemptionDiscountedInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .count();

        // Remise "paiement anticipé" réellement appliquée (§billing, EarlyPaymentDiscountRule) — plus
        // une estimation depuis que ce mécanisme existe pour de vrai (voir applyEarlyPaymentDiscount).
        BigDecimal earlyPaymentDiscountApplied = yearInvoices.stream()
                .filter(i -> i.getDiscountReason() != null && i.getDiscountReason().startsWith(EARLY_PAYMENT_DISCOUNT_REASON_PREFIX))
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

        BigDecimal totalExemptionEstimatedValue = exemptions.stream()
                .filter(e -> e.getType() == ExemptionType.TOTAL)
                .map(e -> estimateAnnualMonthlyFeeValue(e.getStudentId(), e.getFeeTypes()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SubsidyStats(exemptions.size(), totalExemptions, partialExemptions, exemptionDiscountAmount,
                exemptionDiscountedInvoices.size(), exemptionDiscountInvoicePaidCount,
                fullySettled.size(), earlyPaymentDiscountApplied, totalExemptionEstimatedValue);
    }

    /**
     * Estimation de la valeur annuelle de la mensualité d'un élève (§retour fondateurs) — voir
     * {@link SchoolBillingStatsProvider.SubsidyStats#totalExemptionEstimatedValue} pour le pourquoi
     * (une exonération 100% ne produit jamais de facture, donc aucun montant réel à sommer). Somme
     * des échéances RÉELLEMENT configurées pour le frais MENSUALITE de la classe actuelle de l'élève
     * (pas un nombre de mois supposé) ; 0 si l'exonération ne couvre pas la mensualité (§feeTypes), si
     * l'élève n'a pas d'inscription active, ou si sa classe n'a pas de frais MENSUALITE configuré.
     */
    private BigDecimal estimateAnnualMonthlyFeeValue(Long studentId, List<FeeType> feeTypes) {
        if (feeTypes != null && !feeTypes.isEmpty() && !feeTypes.contains(FeeType.MONTHLY)) {
            return BigDecimal.ZERO;
        }
        Long classId = enrollmentQueryPort.findActiveClassId(studentId);
        if (classId == null) return BigDecimal.ZERO;
        return schoolFeeRepository.findByClassIdAndType(classId, FeeType.MONTHLY).stream()
                .flatMap(fee -> installmentRepository.findByFeeId(fee.getId()).stream())
                .map(Installment::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingInstallment> upcomingInstallments(Long tenantId, int daysAhead, int limit) {
        LocalDate today = LocalDate.now();
        List<Installment> installments = installmentRepository.findUpcomingByTenant(
                tenantId, today, today.plusDays(Math.max(1, daysAhead)));
        return installments.stream()
                .limit(Math.max(1, limit))
                .map(i -> new UpcomingInstallment(i.getId(), i.getFeeId(), i.getLabel(), i.getDueDate(), i.getAmount()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> writtenOffAmountByStudent(Long tenantId, Long academicYearId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return Map.of();
        java.util.Set<Long> ids = new java.util.HashSet<>(studentIds);
        return invoiceRepository.findByTenantIdAndAcademicYearId(tenantId, academicYearId).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.CANCELLED && ids.contains(i.getStudentId()))
                .collect(Collectors.groupingBy(Invoice::getStudentId,
                        Collectors.reducing(BigDecimal.ZERO,
                                i -> (i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                                        .subtract(i.getPaidAmount() != null ? i.getPaidAmount() : BigDecimal.ZERO),
                                BigDecimal::add)));
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
        long paidCount = invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count();
        return new BillingStats(invoiced, collected, outstanding, recoveryRate, unpaidCount, paidCount);
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
