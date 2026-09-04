package gn.lacaeducation.platform.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformFeeSettingJpaRepository extends JpaRepository<PlatformFeeSettingJpaEntity, Long> {
    Optional<PlatformFeeSettingJpaEntity> findByActiveTrue();
}
