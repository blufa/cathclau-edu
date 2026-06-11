package gn.lacaeducation.billing.application.port.in;
import gn.lacaeducation.billing.domain.Installment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface InstallmentUseCase {
    record CreateInstallmentCommand(Long feeId, String label, BigDecimal amount, LocalDate dueDate) {}
    record UpdateInstallmentCommand(String label, BigDecimal amount, LocalDate dueDate) {}
    Installment create(CreateInstallmentCommand command);
    Installment update(Long id, UpdateInstallmentCommand command);
    Optional<Installment> findById(Long id);
    List<Installment> findByFeeId(Long feeId);
    void delete(Long id);
}
