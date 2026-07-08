package gn.lacaeducation.accounting.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface ChartOfAccountJpaRepository extends JpaRepository<ChartOfAccountJpaEntity, Long> {
    List<ChartOfAccountJpaEntity> findByTenantId(Long tenantId);
}
