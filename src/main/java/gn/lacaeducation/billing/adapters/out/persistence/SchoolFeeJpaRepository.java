package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.domain.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface SchoolFeeJpaRepository extends JpaRepository<SchoolFeeJpaEntity, Long> {
    List<SchoolFeeJpaEntity> findByTenantId(Long tenantId);
    List<SchoolFeeJpaEntity> findByClassId(Long classId);
    List<SchoolFeeJpaEntity> findByClassIdAndType(Long classId, FeeType type);
    List<SchoolFeeJpaEntity> findByRouteId(Long routeId);
    List<SchoolFeeJpaEntity> findByRouteIdAndType(Long routeId, FeeType type);
    List<SchoolFeeJpaEntity> findByTenantIdAndAcademicYear(Long tenantId, String academicYear);
}
