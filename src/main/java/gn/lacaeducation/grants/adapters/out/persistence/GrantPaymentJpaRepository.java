package gn.lacaeducation.grants.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface GrantPaymentJpaRepository extends JpaRepository<GrantPaymentJpaEntity, Long> {
    List<GrantPaymentJpaEntity> findByGrantIdOrderByPaymentDateDesc(Long grantId);
    void deleteByGrantId(Long grantId);
}
