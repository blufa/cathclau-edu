package gn.lacaeducation.iam.application.port.out;

import gn.lacaeducation.iam.domain.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByToken(String token);
}
