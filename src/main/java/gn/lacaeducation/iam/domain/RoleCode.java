package gn.lacaeducation.iam.domain;

public enum RoleCode {
    SUPER_ADMIN, SCHOOL_ADMIN, DIRECTOR, RESPONSABLE_PEDAGOGIQUE,
    ACCOUNTANT, SECRETARY, TEACHER, PARENT, STUDENT,
    /** Compte plateforme (hors tenant) limité à la gestion du blog d'entreprise : ne doit jamais avoir
     * accès aux écoles, abonnements ou données clients. */
    REDACTEUR,
    /** Direction Préfectorale ou Communale de l'Éducation (DPE/DCE) : lecture seule, cross-tenant mais
     * strictement scopée au périmètre de sa préfecture/commune (voir DpeScopeContext). N'est jamais
     * inclus dans l'impersonation SUPER_ADMIN car son périmètre est résolu différemment (liste de
     * tenants explicite, pas un bypass total du filtre tenant). */
    DPE_ADMIN
}
