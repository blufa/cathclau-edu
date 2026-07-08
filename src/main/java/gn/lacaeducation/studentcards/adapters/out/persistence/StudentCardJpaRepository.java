package gn.lacaeducation.studentcards.adapters.out.persistence;
import gn.lacaeducation.studentcards.domain.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface StudentCardJpaRepository extends JpaRepository<StudentCardJpaEntity, Long> {
    List<StudentCardJpaEntity> findByTenantId(Long tenantId);
    List<StudentCardJpaEntity> findByStudentId(Long studentId);
    Optional<StudentCardJpaEntity> findByStudentIdAndStatus(Long studentId, CardStatus status);
    Optional<StudentCardJpaEntity> findByQrPayload(String qrPayload);
    long countByTenantId(Long tenantId);
}
