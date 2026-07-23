package gn.lacaeducation.billing.application.port.in;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.SchoolFee;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface SchoolFeeUseCase {
    record CreateFeeCommand(Long tenantId, Long classId, FeeType type, BigDecimal amount, String academicYear, String label) {}
    record UpdateFeeCommand(Long classId, FeeType type, BigDecimal amount, String academicYear, String label) {}
    SchoolFee create(CreateFeeCommand command);
    SchoolFee update(Long id, UpdateFeeCommand command);
    Optional<SchoolFee> findById(Long id);
    List<SchoolFee> findByTenantId(Long tenantId);
    List<SchoolFee> findByClassId(Long classId);
    List<SchoolFee> findByTenantIdAndAcademicYear(Long tenantId, String academicYear);
    void delete(Long id);
    boolean existsById(Long id);
}
