package gn.lacaeducation.platform.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenJpaEntity, Long> {
    Optional<EmailVerificationTokenJpaEntity> findByToken(String token);
}
