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
}
