package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.TenantStatus;
import jakarta.persistence.Column;
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

    @Column(name = "current_academic_year")
    private String currentAcademicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
