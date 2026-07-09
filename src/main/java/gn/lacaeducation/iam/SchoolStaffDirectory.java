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
}
