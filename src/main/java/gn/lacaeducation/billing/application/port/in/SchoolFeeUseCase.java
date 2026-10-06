package gn.lacaeducation.billing.application.port.in;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.SchoolFee;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface SchoolFeeUseCase {
    /** {@code routeId} : ligne de transport pour un frais TRANSPORT — remplace classId dans ce cas (§billing). */
    record CreateFeeCommand(Long tenantId, Long classId, Long routeId, FeeType type, BigDecimal amount, String academicYear, String label) {}
    record UpdateFeeCommand(Long classId, Long routeId, FeeType type, BigDecimal amount, String academicYear, String label) {}
    /** {@code createdBy} : null pour une création automatique par le système (ex. frais de
     * transport/cours, voir CourseBillingApiImpl) — jamais null depuis l'écran "Frais scolaires". */
    SchoolFee create(CreateFeeCommand command, Long createdBy);
    SchoolFee update(Long id, UpdateFeeCommand command);
    Optional<SchoolFee> findById(Long id);
    List<SchoolFee> findByTenantId(Long tenantId);
    List<SchoolFee> findByClassId(Long classId);
    List<SchoolFee> findByRouteId(Long routeId);
    List<SchoolFee> findByTenantIdAndAcademicYear(Long tenantId, String academicYear);
    /** {@code deletedBy} : qui a supprimé ce frais — tracé dans le journal d'audit pour distinguer
     * une suppression faite dans l'application d'une disparition inexpliquée (§retour fondateurs). */
    void delete(Long id, Long deletedBy);
    boolean existsById(Long id);
}
