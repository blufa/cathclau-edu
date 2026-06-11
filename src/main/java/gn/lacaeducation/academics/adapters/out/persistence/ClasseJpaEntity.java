package gn.lacaeducation.academics.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "school_classes")
@Getter @Setter @NoArgsConstructor
public class ClasseJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "level_id") private Long levelId;
    @Column(name = "academic_year_id", nullable = false) private Long academicYearId;
    @Column(name = "head_teacher_id") private Long headTeacherId;
    @Column(name = "room_id") private Long roomId;
    @Column(nullable = false) private String label;
    private Integer headcount;
    @Column(name = "max_capacity") private Integer maxCapacity;
}
