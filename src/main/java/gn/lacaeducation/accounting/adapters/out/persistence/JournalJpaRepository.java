package gn.lacaeducation.accounting.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface JournalJpaRepository extends JpaRepository<JournalJpaEntity, Long> {
    List<JournalJpaEntity> findByTenantId(Long tenantId);
}
