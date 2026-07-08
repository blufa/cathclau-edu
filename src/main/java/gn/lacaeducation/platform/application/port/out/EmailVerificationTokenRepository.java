package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.EmailVerificationToken;

import java.util.Optional;

public interface EmailVerificationTokenRepository {
    EmailVerificationToken save(EmailVerificationToken token);
    Optional<EmailVerificationToken> findByToken(String token);
}
