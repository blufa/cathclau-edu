package gn.lacaeducation.health.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface InfirmaryVisitJpaRepository extends JpaRepository<InfirmaryVisitJpaEntity, Long> {
    List<InfirmaryVisitJpaEntity> findByTenantIdOrderByVisitDateDesc(Long tenantId);
    List<InfirmaryVisitJpaEntity> findByStudentIdOrderByVisitDateDesc(Long studentId);
}
