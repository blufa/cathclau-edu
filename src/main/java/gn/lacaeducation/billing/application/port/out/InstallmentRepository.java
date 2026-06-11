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
    void deleteById(Long id);
}
