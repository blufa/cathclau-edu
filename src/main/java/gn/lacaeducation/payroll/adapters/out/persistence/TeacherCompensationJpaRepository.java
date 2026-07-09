package gn.lacaeducation.payroll.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface TeacherCompensationJpaRepository extends JpaRepository<TeacherCompensationJpaEntity, Long> {
    Optional<TeacherCompensationJpaEntity> findByTeacherId(Long teacherId);
    List<TeacherCompensationJpaEntity> findByTenantId(Long tenantId);
}
