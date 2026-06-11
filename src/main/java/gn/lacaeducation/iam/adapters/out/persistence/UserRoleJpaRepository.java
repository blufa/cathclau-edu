package gn.lacaeducation.iam.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, Long> {
}
