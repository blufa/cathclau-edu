package gn.lacaeducation.billing.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ClassTransferRefundJpaRepository extends JpaRepository<ClassTransferRefundJpaEntity, Long> {
    List<ClassTransferRefundJpaEntity> findByStudentId(Long studentId);
}
