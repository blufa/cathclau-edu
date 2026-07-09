package gn.lacaeducation.iam.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private Long tenantId;
    private String firstName;
    private String lastName;
    /** Identifiant de connexion généré (prenom.nom) pour les comptes créés par l'école. */
    private String username;
    private String email;
    private String passwordHash;
    /** Impose le changement du mot de passe (temporaire) à la première connexion. */
    private boolean mustChangePassword;
    private String phone;
    private Boolean active;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private int failedAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime deletedAt;

    /**
     * Mot de passe temporaire en clair, uniquement au moment de la création/réinitialisation, pour
     * le restituer une seule fois à l'administrateur. Jamais persisté (non mappé en base).
     */
    private transient String plainTemporaryPassword;
}
