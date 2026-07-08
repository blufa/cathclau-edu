package gn.lacaeducation.pettycash.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface PettyCashFundJpaRepository extends JpaRepository<PettyCashFundJpaEntity, Long> {
    List<PettyCashFundJpaEntity> findByTenantId(Long tenantId);
}
