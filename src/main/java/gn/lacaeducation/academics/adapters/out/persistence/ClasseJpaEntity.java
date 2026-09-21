package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.domain.valueobject.ClassType;
import gn.lacaeducation.academics.domain.valueobject.Grade;
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

@Entity
@Table(name = "school_classes")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
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
    @Enumerated(EnumType.STRING) @Column(name = "class_type", nullable = false)
    private ClassType classType;
    @Column(name = "class_representative_student_id") private Long classRepresentativeStudentId;
    @Enumerated(EnumType.STRING) @Column(name = "grade") private Grade grade;
    @Column(name = "serie") private String serie;
}
