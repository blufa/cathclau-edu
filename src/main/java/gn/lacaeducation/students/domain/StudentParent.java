package gn.lacaeducation.students.domain;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentParent {
    private Long id;
    private Long studentId;
    private Long parentId;
    private Relationship relationship;
    /** Responsable principal (« responsable 1 ») : cible des factures, relances et notifications. */
    private boolean primaryGuardian;
}
