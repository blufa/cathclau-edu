package gn.lacaeducation.students.domain;

/** Type de fiche personnel référencée par {@link Parent#getLinkedStaffId()} — lien direct sans compte
 *  de connexion requis (voir {@code StaffChildBenefitService#isStaffChild}). */
public enum StaffLinkType {
    TEACHER, EMPLOYEE
}
