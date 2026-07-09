package gn.lacaeducation.expenses.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface AnnualBudgetJpaRepository extends JpaRepository<AnnualBudgetJpaEntity, Long> {
    List<AnnualBudgetJpaEntity> findByTenantIdOrderByAcademicYearDesc(Long tenantId);
}
