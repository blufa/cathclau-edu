package gn.lacaeducation.billing.application.service;
import gn.lacaeducation.billing.application.port.in.InstallmentUseCase;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.domain.Installment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class InstallmentService implements InstallmentUseCase {
    private final InstallmentRepository repo;
    @Override public Installment create(CreateInstallmentCommand c) {
        return repo.save(Installment.builder().feeId(c.feeId()).label(c.label()).amount(c.amount()).dueDate(c.dueDate()).build());
    }
    @Override public Installment update(Long id, UpdateInstallmentCommand c) {
        Installment i = load(id); i.setLabel(c.label()); i.setAmount(c.amount()); i.setDueDate(c.dueDate());
        return repo.save(i);
    }
    @Override @Transactional(readOnly=true) public Optional<Installment> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<Installment> findByFeeId(Long feeId) { return repo.findByFeeId(feeId); }
    @Override public void delete(Long id) { repo.deleteById(id); }
    private Installment load(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Installment not found: " + id)); }
}
