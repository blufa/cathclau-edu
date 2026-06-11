package gn.lacaeducation.billing.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface SchoolFeeJpaRepository extends JpaRepository<SchoolFeeJpaEntity, Long> {
    List<SchoolFeeJpaEntity> findByTenantId(Long tenantId);
    List<SchoolFeeJpaEntity> findByClassId(Long classId);
    List<SchoolFeeJpaEntity> findByTenantIdAndAcademicYear(Long tenantId, String academicYear);
}
