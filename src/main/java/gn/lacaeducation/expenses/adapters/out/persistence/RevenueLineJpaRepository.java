package gn.lacaeducation.expenses.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface RevenueLineJpaRepository extends JpaRepository<RevenueLineJpaEntity, Long> {
    List<RevenueLineJpaEntity> findBySheetId(Long sheetId);
}
