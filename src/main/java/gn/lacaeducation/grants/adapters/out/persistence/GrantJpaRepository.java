package gn.lacaeducation.grants.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface GrantJpaRepository extends JpaRepository<GrantJpaEntity, Long> {
    List<GrantJpaEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
