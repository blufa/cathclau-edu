package gn.lacaeducation.students.domain;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Student {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String studentNumber;
    private String photo;
    private String firstName;
    private String lastName;
    private Gender gender;
    private LocalDate birthDate;
    private String birthPlace;
    private String address;
    private String phone;
    private String email;
    private String nationality;
    private String originSchool;
    // Identity extras
    private String birthCertificateNumber;
    private String bloodGroup;
    // Address extras
    private String neighborhood;
    private String landmark;
    // Previous schooling
    private String previousClass;
    private Boolean repeating;
    private String transferReason;
    // Health
    private String medicalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    /** Clé d'idempotence fournie par le client (mode hors-ligne) : rejouer avec la même clé après une
     *  coupure réseau renvoie cet élève au lieu d'en créer un second. Null pour une création en ligne. */
    private String idempotencyKey;
}
