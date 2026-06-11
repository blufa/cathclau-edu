package gn.lacaeducation.students.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface ParentJpaRepository extends JpaRepository<ParentJpaEntity, Long> {
    List<ParentJpaEntity> findByTenantId(Long tenantId);
}
