package gn.lacaeducation.payroll.domain;

/**
 * Nature du salarié rattaché à une fiche de paie (§7.1). Les fiches d'enseignants s'appuient sur le
 * pointage (heures/absences) ; les autres catégories reposent sur un salaire de base + structure.
 */
public enum EmployeeType {
    TEACHER,
    ADMINISTRATIVE,
    DIRECTION,
    SUPPORT
}
