package gn.lacaeducation.iam.application.port.out;

import gn.lacaeducation.iam.domain.PasswordResetOtp;

import java.util.Optional;

public interface PasswordResetOtpRepository {
    PasswordResetOtp save(PasswordResetOtp otp);

    /** Cherche un code actif (non utilisé, non expiré filtré côté appelant) pour ce couple précis —
     * jamais une recherche par code seul (voir {@link PasswordResetOtp}). */
    Optional<PasswordResetOtp> findByUserIdAndCode(Long userId, String code);
}
