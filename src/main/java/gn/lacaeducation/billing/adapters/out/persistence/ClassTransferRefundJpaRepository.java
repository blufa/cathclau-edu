package gn.lacaeducation.billing.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ClassTransferRefundJpaRepository extends JpaRepository<ClassTransferRefundJpaEntity, Long> {
    List<ClassTransferRefundJpaEntity> findByStudentId(Long studentId);
    /** Filtre Hibernate tenantFilter déjà actif sur l'entité — pas de tenantId explicite nécessaire. */
    List<ClassTransferRefundJpaEntity> findByRefundedAtIsNullOrderByCreatedAtAsc();
}
