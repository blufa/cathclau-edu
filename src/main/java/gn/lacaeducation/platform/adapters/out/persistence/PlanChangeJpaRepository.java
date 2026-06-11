package gn.lacaeducation.platform.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface PlanChangeJpaRepository extends JpaRepository<PlanChangeJpaEntity, Long> {
    List<PlanChangeJpaEntity> findBySubscriptionId(Long subscriptionId);
}
