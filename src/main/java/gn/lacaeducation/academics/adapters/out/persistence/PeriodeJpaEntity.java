package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.domain.valueobject.TypePeriode;
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

import java.time.LocalDate;

@Entity
@Table(name = "periods")
@Getter @Setter @NoArgsConstructor
public class PeriodeJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "academic_year_id", nullable = false) private Long academicYearId;
    @Column(nullable = false) private String label;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TypePeriode type;
    @Column(name = "sort_order") private Integer sortOrder;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    private Boolean closed;
}
