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
    /** Exclut CET enfant précis de la retenue salariale automatique de ce parent (s'il est membre du
     *  personnel) — contrairement à {@link Parent#isExcludeFromAutoDeduction()} (global, tous les
     *  enfants de ce parent), ce réglage est par lien élève-parent : un même salarié peut avoir un
     *  enfant à charge de l'avantage personnel et un autre payé directement par un parent (§retour
     *  comptable, voir ParentAccessProvider#hasOptedOutOfStaffAutoDeduction, OU des deux réglages). */
    private boolean excludedFromStaffDeduction;
}
