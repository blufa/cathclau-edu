package gn.lacaeducation.iam.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PasswordResetOtpJpaRepository extends JpaRepository<PasswordResetOtpJpaEntity, Long> {
    Optional<PasswordResetOtpJpaEntity> findByUserIdAndCode(Long userId, String code);
}
