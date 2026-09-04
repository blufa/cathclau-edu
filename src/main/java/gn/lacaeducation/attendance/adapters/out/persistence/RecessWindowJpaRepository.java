package gn.lacaeducation.attendance.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface RecessWindowJpaRepository extends JpaRepository<RecessWindowJpaEntity, Long> {
    List<RecessWindowJpaEntity> findByTenantId(Long tenantId);
}
