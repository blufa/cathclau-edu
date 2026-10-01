package gn.lacaeducation.students.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface ParentJpaRepository extends JpaRepository<ParentJpaEntity, Long> {
    List<ParentJpaEntity> findByTenantId(Long tenantId);
    Optional<ParentJpaEntity> findByUserId(Long userId);
    List<ParentJpaEntity> findAllByUserId(Long userId);
    List<ParentJpaEntity> findAllByLinkedStaffTypeAndLinkedStaffId(
            gn.lacaeducation.students.domain.StaffLinkType linkedStaffType, Long linkedStaffId);
    Optional<ParentJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
