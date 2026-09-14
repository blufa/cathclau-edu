package gn.lacaeducation.billing.application.port.out;
import gn.lacaeducation.billing.domain.Installment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface InstallmentRepository {
    Installment save(Installment installment);
    Optional<Installment> findById(Long id);
    List<Installment> findByFeeId(Long feeId);
    List<Installment> findOverdue(LocalDate today);
    /** Échéances PENDING d'un tenant dont la date limite tombe dans [from, to], triées par date croissante. */
    List<Installment> findUpcomingByTenant(Long tenantId, LocalDate from, LocalDate to);
    void deleteById(Long id);
}
