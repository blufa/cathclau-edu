package gn.lacaeducation.communication.adapters.out.persistence;

import gn.lacaeducation.communication.application.port.out.PaymentReminderDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PaymentReminderDataAdapter implements PaymentReminderDataPort {

    private final PaymentReminderJpaRepository repo;

    @Override
    public List<DueLine> findOverdueLines(Long tenantId, LocalDate today) {
        return repo.findOverdueLines(tenantId, today);
    }

    @Override
    public List<DueLine> findUpcomingLines(Long tenantId, LocalDate from, LocalDate to) {
        return repo.findUpcomingLines(tenantId, from, to);
    }

    @Override
    public List<ClassFeeLine> findClassMonthlyFeeLines(Long tenantId, Long classId) {
        return repo.findClassMonthlyFeeLines(tenantId, classId);
    }

    @Override
    public int countMonthlyTranches(Long tenantId, Long classId) {
        return (int) repo.countMonthlyTranches(tenantId, classId);
    }
}
