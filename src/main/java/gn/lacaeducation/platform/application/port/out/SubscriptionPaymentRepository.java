package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.SubscriptionPayment;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPaymentRepository {
    SubscriptionPayment save(SubscriptionPayment payment);
    Optional<SubscriptionPayment> findById(Long id);
    List<SubscriptionPayment> findAll();
    void deleteById(Long id);
    boolean existsById(Long id);
}
