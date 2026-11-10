package gn.lacaeducation.studentcards.application.port.out;
import gn.lacaeducation.studentcards.domain.CardStatus;
import gn.lacaeducation.studentcards.domain.StudentCard;
import java.util.List;
import java.util.Optional;
public interface StudentCardRepository {
    StudentCard save(StudentCard c);
    Optional<StudentCard> findById(Long id);
    List<StudentCard> findByTenantId(Long tenantId);
    List<StudentCard> findByStudentId(Long studentId);
    Optional<StudentCard> findByStudentIdAndStatus(Long studentId, CardStatus status);
    Optional<StudentCard> findByQrPayload(String qrPayload);
    long countByTenantId(Long tenantId);
    void deleteById(Long id);
    boolean existsByTenantIdAndCardNumber(Long tenantId, String cardNumber);
}
