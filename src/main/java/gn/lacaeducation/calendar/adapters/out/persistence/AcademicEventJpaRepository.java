package gn.lacaeducation.calendar.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AcademicEventJpaRepository extends JpaRepository<AcademicEventJpaEntity, Long> {
    List<AcademicEventJpaEntity> findByTenantIdOrderByStartDateAsc(Long tenantId);
    List<AcademicEventJpaEntity> findByTenantIdAndAcademicYearIdOrderByStartDateAsc(Long tenantId, Long academicYearId);
}
