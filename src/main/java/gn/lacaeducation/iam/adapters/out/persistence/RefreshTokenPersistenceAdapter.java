package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.application.port.out.RefreshTokenRepository;
import gn.lacaeducation.iam.domain.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity = toEntity(token);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public void revokeByToken(String token) {
        jpaRepository.findByToken(token).ifPresent(e -> {
            e.setRevoked(true);
            jpaRepository.save(e);
        });
    }

    @Override
    public void revokeAllByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .token(e.getToken())
                .expiresAt(e.getExpiresAt())
                .revoked(e.isRevoked())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private RefreshTokenJpaEntity toEntity(RefreshToken d) {
        RefreshTokenJpaEntity e = new RefreshTokenJpaEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setToken(d.getToken());
        e.setExpiresAt(d.getExpiresAt());
        e.setRevoked(d.isRevoked());
        return e;
    }
}
