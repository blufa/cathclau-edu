package gn.lacaeducation.expenses.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface SupplierJpaRepository extends JpaRepository<SupplierJpaEntity, Long> {
    List<SupplierJpaEntity> findByTenantId(Long tenantId);
}
