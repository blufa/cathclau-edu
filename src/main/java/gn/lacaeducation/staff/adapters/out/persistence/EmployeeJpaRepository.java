package gn.lacaeducation.staff.adapters.out.persistence;

import gn.lacaeducation.staff.domain.EmployeeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, Long> {
    List<EmployeeJpaEntity> findByTenantId(Long tenantId);
    List<EmployeeJpaEntity> findByTenantIdAndCategory(Long tenantId, EmployeeCategory category);
    boolean existsByTenantIdAndEmployeeNumber(Long tenantId, String employeeNumber);
    Optional<EmployeeJpaEntity> findByUserId(Long userId);
    Optional<EmployeeJpaEntity> findByTenantIdAndLinkedTeacherId(Long tenantId, Long linkedTeacherId);
    @Query("SELECT e FROM EmployeeJpaEntity e WHERE e.employeeNumber IS NULL OR e.employeeNumber = ''")
    List<EmployeeJpaEntity> findMissingEmployeeNumber();
}
