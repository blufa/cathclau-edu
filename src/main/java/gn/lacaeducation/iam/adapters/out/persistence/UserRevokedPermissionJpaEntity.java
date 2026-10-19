package gn.lacaeducation.iam.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pendant soustractif de {@link UserPermissionJpaEntity} : une permission listée ici est retirée à
 * CET utilisateur précis même si son rôle (partagé par toute la plateforme, voir Role) l'accorde —
 * voir {@code AuthService#permissionsOf}. */
@Entity
@Table(name = "user_revoked_permissions")
@Getter
@Setter
@NoArgsConstructor
public class UserRevokedPermissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;
}
