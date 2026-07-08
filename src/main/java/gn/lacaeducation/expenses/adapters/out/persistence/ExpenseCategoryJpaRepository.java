package gn.lacaeducation.expenses.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface ExpenseCategoryJpaRepository extends JpaRepository<ExpenseCategoryJpaEntity, Long> {
    List<ExpenseCategoryJpaEntity> findByTenantId(Long tenantId);
}
