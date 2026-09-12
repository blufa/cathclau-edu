package gn.lacaeducation.staff.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Teacher {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String staffNumber;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String photo;
    private String specialization;
    private LocalDate hireDate;
    private Boolean active;
    // Professional dossier (Guinean private schools)
    private String employmentStatus;            // PERMANENT | VACATAIRE | CONTRACTUEL
    private String diploma;
    private String remunerationType;            // MENSUEL | HORAIRE | FORFAIT
    private BigDecimal remunerationRate;
    private String teachingAuthorizationNumber; // n° autorisation SNEPUP
    private String cnssNumber;
    /** Compte bancaire (RIB/IBAN) optionnel — pour le fichier bancaire de paie. Le téléphone
     *  ({@link #phone}) reste prioritaire pour le virement Mobile Money. */
    private String bankAccount;
    /** Clé d'idempotence fournie par le client (mode hors-ligne) : rejouer avec la même clé après une
     *  coupure réseau renvoie cet enseignant au lieu d'en créer un second. Null pour une création en ligne. */
    private String idempotencyKey;
}
