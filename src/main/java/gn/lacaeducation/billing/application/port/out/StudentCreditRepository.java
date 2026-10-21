package gn.lacaeducation.billing.application.port.out;

import gn.lacaeducation.billing.domain.StudentCredit;
import gn.lacaeducation.billing.domain.StudentCreditUsage;

import java.util.List;
import java.util.Optional;

public interface StudentCreditRepository {
    StudentCredit save(StudentCredit credit);
    Optional<StudentCredit> findById(Long id);
    /** Verrou pessimiste — voir {@code StudentCreditJpaRepository#lockById}. */
    Optional<StudentCredit> lockById(Long id);
    List<StudentCredit> findByStudentId(Long studentId);

    StudentCreditUsage saveUsage(StudentCreditUsage usage);
    List<StudentCreditUsage> findUsagesByStudentId(Long studentId);
    List<StudentCreditUsage> findUsagesByCreditId(Long creditId);
}
