package gn.lacaeducation.iam.application.port.out;

import gn.lacaeducation.iam.domain.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByToken(String token);
    void revokeByToken(String token);
    void revokeAllByUserId(Long userId);
}
