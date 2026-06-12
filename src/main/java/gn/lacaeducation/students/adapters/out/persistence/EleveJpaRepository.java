package gn.lacaeducation.students.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface EleveJpaRepository extends JpaRepository<EleveJpaEntity, Long> {
    List<EleveJpaEntity> findByTenantId(Long tenantId);
    long countByTenantId(Long tenantId);
    boolean existsByTenantIdAndStudentNumber(Long tenantId, String studentNumber);
}
