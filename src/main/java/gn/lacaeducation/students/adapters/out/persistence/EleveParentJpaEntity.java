package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.domain.Relationship;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "student_parents")
@Getter @Setter @NoArgsConstructor
class EleveParentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "parent_id", nullable = false) private Long parentId;
    @Enumerated(EnumType.STRING) private Relationship relationship;
    @Column(name = "primary_guardian", nullable = false) private boolean primaryGuardian;
    @Column(name = "excluded_from_staff_deduction", nullable = false) private boolean excludedFromStaffDeduction;
}
