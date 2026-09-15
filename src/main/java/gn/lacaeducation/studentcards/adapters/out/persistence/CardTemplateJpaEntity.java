package gn.lacaeducation.studentcards.adapters.out.persistence;
import gn.lacaeducation.studentcards.domain.BadgeKind;
import gn.lacaeducation.studentcards.domain.CardOrientation;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
@Entity @Table(name = "card_template") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class CardTemplateJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String name;
    @Column(name = "logo_url") private String logoUrl;
    @Column(name = "primary_color") private String primaryColor;
    @Column(name = "secondary_color") private String secondaryColor;
    @Column(name = "background_url") private String backgroundUrl;
    @Enumerated(EnumType.STRING) @Column(length = 12) private CardOrientation orientation;
    @Column(name = "recto_config", columnDefinition = "text") private String rectoConfig;
    @Column(name = "verso_config", columnDefinition = "text") private String versoConfig;
    @Column(name = "is_default") private Boolean isDefault;
    private Boolean active;
    @Enumerated(EnumType.STRING) @Column(name = "badge_kind", nullable = false, length = 10) private BadgeKind badgeKind;
}
