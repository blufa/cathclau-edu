package gn.lacaeducation.grants.application.port.out;
import gn.lacaeducation.grants.domain.GrantPayment;
import java.util.List;
import java.util.Optional;
public interface GrantPaymentRepository {
    GrantPayment save(GrantPayment p);
    Optional<GrantPayment> findById(Long id);
    List<GrantPayment> findByGrantId(Long grantId);
    void deleteById(Long id);
    void deleteByGrantId(Long grantId);
}
