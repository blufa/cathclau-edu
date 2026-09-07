package gn.lacaeducation.iam;

import java.util.List;

/**
 * API publique du module iam pour joindre le personnel encadrant d'une école (par email),
 * indépendamment des codes de rôles internes au module.
 */
public interface SchoolStaffDirectory {

    /** Un contact joignable du personnel. */
    record SchoolStaffContact(String email, String fullName) {}

    /**
     * Destinataires à alerter lors d'une modification de note : l'administration de l'école
     * (ADMIN_ECOLE / SCHOOL_ADMIN) et le responsable pédagogique, avec un email renseigné.
     */
    List<SchoolStaffContact> findGradeChangeRecipients(Long tenantId);

    /** Un compte SCHOOL_ADMIN de l'école, avec son numéro de téléphone (pour alerte SMS). */
    record SchoolAdminContact(String phone, String fullName) {}

    /**
     * Compte(s) SCHOOL_ADMIN de l'école — utilisé pour alerter par SMS le fondateur/administrateur
     * de chaque paiement reçu (§communication), en l'absence pour l'instant d'un numéro de fondateur
     * dédié dans les paramètres du tenant.
     */
    List<SchoolAdminContact> findSchoolAdminContacts(Long tenantId);

    /** Un membre de la direction, joignable par téléphone, avec sa fonction. */
    record LeadershipContact(String phone, String fullName, String role) {}

    /**
     * Direction de l'école — DIRECTEUR et responsable pédagogique, avec leur téléphone — utilisé par
     * le portail parent pour afficher les contacts de l'établissement (§portal).
     */
    List<LeadershipContact> findLeadershipContacts(Long tenantId);
}
