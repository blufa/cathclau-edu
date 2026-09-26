package gn.lacaeducation.expenses.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface ExpenseJpaRepository extends JpaRepository<ExpenseJpaEntity, Long> {
    List<ExpenseJpaEntity> findBySheetId(Long sheetId);
    List<ExpenseJpaEntity> findByTenantId(Long tenantId);
}
