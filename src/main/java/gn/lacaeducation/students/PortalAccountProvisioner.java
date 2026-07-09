package gn.lacaeducation.students;

/**
 * SPI : création d'un compte de connexion au portail pour un élève. Implémenté par le module
 * {@code iam}. Le {@code username} est le <b>matricule sans tirets</b> — l'élève se connecte donc
 * avec son matricule (pas besoin d'email). Renvoie l'identifiant du compte créé.
 */
public interface PortalAccountProvisioner {

    /**
     * @param tenantId    l'école
     * @param firstName   prénom (affichage)
     * @param lastName    nom (affichage)
     * @param username    identifiant de connexion souhaité (matricule sans tirets) ; rendu unique si besoin
     * @param rawPassword mot de passe en clair (haché par iam)
     * @return l'id du compte utilisateur créé
     */
    Long createStudentLogin(Long tenantId, String firstName, String lastName, String username, String rawPassword);
}
