package gn.lacaeducation.expenses.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface MonthlyExpenseSheetJpaRepository extends JpaRepository<MonthlyExpenseSheetJpaEntity, Long> {
    List<MonthlyExpenseSheetJpaEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
