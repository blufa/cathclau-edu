package gn.lacaeducation.accounting.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface TreasuryAccountJpaRepository extends JpaRepository<TreasuryAccountJpaEntity, Long> {
    List<TreasuryAccountJpaEntity> findByTenantId(Long tenantId);
}
