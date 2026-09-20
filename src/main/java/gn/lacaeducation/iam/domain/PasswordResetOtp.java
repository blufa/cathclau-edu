package gn.lacaeducation.iam.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Code OTP à 6 chiffres pour réinitialiser un mot de passe par SMS — alternative au lien email
 * ({@link PasswordResetToken}) pour les utilisateurs sans email (la majorité : parents, enseignants,
 * personnel se connectent par numéro de téléphone). Toujours scopé par {@code userId} (jamais unique
 * globalement) : deux utilisateurs différents peuvent recevoir le même code au même moment sans
 * collision, la vérification filtre toujours par (userId, code) ensemble. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetOtp {
    private Long id;
    private Long userId;
    private String code;
    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime createdAt;
}
