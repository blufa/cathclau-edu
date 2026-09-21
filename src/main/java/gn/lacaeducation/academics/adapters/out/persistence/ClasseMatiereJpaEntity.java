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
@Table(name = "class_subjects")
@Getter @Setter @NoArgsConstructor
public class ClasseMatiereJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "class_id", nullable = false) private Long schoolClassId;
    @Column(name = "subject_id", nullable = false) private Long subjectId;
    @Column(name = "teacher_id") private Long teacherId;
    @Column(name = "coefficient") private java.math.BigDecimal coefficient;
    @Column(name = "code") private String code;
    @Column(name = "is_specialty", nullable = false) private boolean specialty;
}
