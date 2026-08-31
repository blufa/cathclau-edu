package gn.lacaeducation.audit.adapters.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, Long> {
    Page<AuditLogJpaEntity> findByTenantIdIsNull(Pageable pageable);
}
