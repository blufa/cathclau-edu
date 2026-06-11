package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.application.port.out.PasswordResetTokenRepository;
import gn.lacaeducation.iam.domain.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return toDomain(jpaRepository.save(toEntity(token)));
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    private PasswordResetToken toDomain(PasswordResetTokenJpaEntity e) {
        return PasswordResetToken.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .token(e.getToken())
                .expiresAt(e.getExpiresAt())
                .used(e.isUsed())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private PasswordResetTokenJpaEntity toEntity(PasswordResetToken d) {
        PasswordResetTokenJpaEntity e = new PasswordResetTokenJpaEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setToken(d.getToken());
        e.setExpiresAt(d.getExpiresAt());
        e.setUsed(d.isUsed());
        return e;
    }
}
