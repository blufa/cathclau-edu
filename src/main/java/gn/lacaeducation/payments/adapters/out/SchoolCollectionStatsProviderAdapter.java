package gn.lacaeducation.payments.adapters.out;

import gn.lacaeducation.payments.SchoolCollectionStatsProvider;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Agrège les encaissements réussis d'un tenant par mois civil (§10.2). */
@Component
@RequiredArgsConstructor
class SchoolCollectionStatsProviderAdapter implements SchoolCollectionStatsProvider {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyCollection> monthlyCollections(Long tenantId, int months) {
        int window = months > 0 ? months : 6;
        YearMonth current = YearMonth.now();
        YearMonth start = current.minusMonths(window - 1L);

        Map<YearMonth, BigDecimal> byMonth = paymentRepository
                .findByTenantIdAndStatus(tenantId, PaymentStatus.SUCCEEDED).stream()
                .filter(p -> p.getPaidAt() != null && p.getAmount() != null)
                .collect(Collectors.groupingBy(
                        p -> YearMonth.from(p.getPaidAt()),
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));

        List<MonthlyCollection> result = new ArrayList<>();
        for (YearMonth ym = start; !ym.isAfter(current); ym = ym.plusMonths(1)) {
            result.add(new MonthlyCollection(ym.getYear(), ym.getMonthValue(),
                    byMonth.getOrDefault(ym, BigDecimal.ZERO)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal collectedTotal(Long tenantId, int year, int month) {
        YearMonth target = YearMonth.of(year, month);
        return paymentRepository.findByTenantIdAndStatus(tenantId, PaymentStatus.SUCCEEDED).stream()
                .filter(p -> p.getPaidAt() != null && p.getAmount() != null && YearMonth.from(p.getPaidAt()).equals(target))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyCollectionLine> dailyCollectionLines(Long tenantId, LocalDate date) {
        return paymentRepository.findByPaidAtBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay().minusNanos(1))
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                .filter(p -> tenantId.equals(p.getTenantId()))
                .map(p -> new DailyCollectionLine(p.getId(), p.getStudentId(), p.getAmount(),
                        p.getChannel() != null ? p.getChannel().name() : null, p.getReference()))
                .toList();
    }
}
