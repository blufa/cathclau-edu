package gn.lacaeducation.platform.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SubscriptionInvoiceJpaRepository extends JpaRepository<SubscriptionInvoiceJpaEntity, Long> {
    Optional<SubscriptionInvoiceJpaEntity> findFirstBySubscriptionIdOrderByIssuedAtDesc(Long subscriptionId);
}
