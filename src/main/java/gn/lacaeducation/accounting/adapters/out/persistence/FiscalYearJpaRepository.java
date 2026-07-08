package gn.lacaeducation.accounting.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface FiscalYearJpaRepository extends JpaRepository<FiscalYearJpaEntity, Long> {
    List<FiscalYearJpaEntity> findByTenantId(Long tenantId);
}
