package gn.lacaeducation.audit.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
// Filtre appliqué au tenant courant quand il est renseigné (vue école) ; désactivé pour SUPER_ADMIN
// (TenantContext nul), qui accède sans restriction via findPlatformActions()/findAll().
@Filter(name = "tenantFilter")
public class AuditLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(nullable = false, length = 60)
    private String ressource;

    @Column(name = "ressource_id")
    private Long ressourceId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "adresse_ip", length = 64)
    private String adresseIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
