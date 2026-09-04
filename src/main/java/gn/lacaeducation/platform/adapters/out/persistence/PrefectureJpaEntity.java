package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.PrefectureStatus;
import gn.lacaeducation.platform.domain.PrefectureType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prefectures")
@Getter
@Setter
@NoArgsConstructor
public class PrefectureJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrefectureType type;

    @Column(length = 150)
    private String region;

    @Column(nullable = false, length = 100)
    private String pays;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PrefectureStatus statut;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
