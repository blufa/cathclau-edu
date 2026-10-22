package gn.lacaeducation.billing.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface StudentCreditUsageJpaRepository extends JpaRepository<StudentCreditUsageJpaEntity, Long> {
    List<StudentCreditUsageJpaEntity> findByStudentIdOrderByAppliedAtDesc(Long studentId);
    List<StudentCreditUsageJpaEntity> findByCreditId(Long creditId);
}
