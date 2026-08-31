package gn.lacaeducation.iam.domain;

public enum RoleCode {
    SUPER_ADMIN, SCHOOL_ADMIN, DIRECTOR, RESPONSABLE_PEDAGOGIQUE,
    ACCOUNTANT, SECRETARY, TEACHER, PARENT, STUDENT,
    /** Compte plateforme (hors tenant) limité à la gestion du blog d'entreprise : ne doit jamais avoir
     * accès aux écoles, abonnements ou données clients. */
    REDACTEUR
}
