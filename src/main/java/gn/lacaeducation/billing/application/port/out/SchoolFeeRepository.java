package gn.lacaeducation.billing.application.port.out;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.SchoolFee;
import java.util.List;
import java.util.Optional;
public interface SchoolFeeRepository {
    SchoolFee save(SchoolFee fee);
    Optional<SchoolFee> findById(Long id);
    List<SchoolFee> findByTenantId(Long tenantId);
    List<SchoolFee> findByClassId(Long classId);
    List<SchoolFee> findByClassIdAndType(Long classId, FeeType type);
    List<SchoolFee> findByRouteId(Long routeId);
    /** Frais TRANSPORT configuré(s) pour une ligne — utilisé pour facturer un abonnement transport. */
    List<SchoolFee> findByRouteIdAndType(Long routeId, FeeType type);
    List<SchoolFee> findByTenantIdAndAcademicYear(Long tenantId, String academicYear);
    void deleteById(Long id);
    boolean existsById(Long id);
}
