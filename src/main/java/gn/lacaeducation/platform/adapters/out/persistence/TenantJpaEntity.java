package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.EstablishmentType;
import gn.lacaeducation.platform.domain.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
public class TenantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_name", nullable = false)
    private String schoolName;

    @Column(nullable = false, unique = true)
    private String slug;

    private String logo;

    @Column(name = "brand_color")
    private String brandColor;

    private String address;
    private String city;
    private String country;
    private String phone;
    private String email;

    @Column(name = "website")
    private String website;

    private String director;

    @Column(name = "accreditation_number")
    private String accreditationNumber;

    @Column(name = "opening_authorization")
    private String openingAuthorization;

    /** Un ou plusieurs types (primaire, collège, lycée…) stockés en CSV via le converter. */
    @Convert(converter = EstablishmentTypeSetConverter.class)
    @Column(name = "establishment_types")
    private Set<EstablishmentType> establishmentTypes = new LinkedHashSet<>();

    @Column(name = "current_academic_year")
    private String currentAcademicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @Column(name = "requested_plan_code")
    private String requestedPlanCode;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "onboarded_at")
    private LocalDateTime onboardedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
