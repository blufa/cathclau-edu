-- Réinitialisation de mot de passe par code OTP envoyé par SMS — alternative au lien par email
-- (password_reset_token) pour les utilisateurs sans email (la majorité : connexion par téléphone).
-- Le code n'est JAMAIS unique globalement (voir domain.PasswordResetOtp) : la vérification filtre
-- toujours par (user_id, code) ensemble, deux utilisateurs peuvent recevoir le même code au même
-- instant sans collision.

CREATE TABLE password_reset_otp (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    code       VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   DEFAULT NOW()
);

CREATE INDEX idx_password_reset_otp_user_code ON password_reset_otp (user_id, code);
