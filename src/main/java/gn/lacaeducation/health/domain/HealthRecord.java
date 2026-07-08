package gn.lacaeducation.health.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Fiche santé d'un élève (0..1) : allergies, PAI, contacts d'urgence, médecin traitant. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecord {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private String bloodGroup;
    private String allergies;
    private String chronicConditions;
    private String medications;
    /** Projet d'Accueil Individualisé (PAI) en place ? */
    private boolean pai;
    private String paiDetails;
    private String dietaryRestrictions;
    private String doctorName;
    private String doctorPhone;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String insurance;
    private String notes;
    private LocalDateTime updatedAt;
}
