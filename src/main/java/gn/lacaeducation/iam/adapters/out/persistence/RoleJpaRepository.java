package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.domain.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Long> {

    Optional<RoleJpaEntity> findByCode(RoleCode code);

    @Query("SELECT r FROM RoleJpaEntity r JOIN UserRoleJpaEntity ur ON r.id = ur.roleId WHERE ur.userId = :userId")
    List<RoleJpaEntity> findByUserId(@Param("userId") Long userId);
}
