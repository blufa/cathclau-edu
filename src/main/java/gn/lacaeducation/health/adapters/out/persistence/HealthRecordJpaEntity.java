package gn.lacaeducation.health.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_records")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class HealthRecordJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "blood_group", length = 8) private String bloodGroup;
    @Column(columnDefinition = "text") private String allergies;
    @Column(name = "chronic_conditions", columnDefinition = "text") private String chronicConditions;
    @Column(columnDefinition = "text") private String medications;
    @Column(nullable = false) private boolean pai;
    @Column(name = "pai_details", columnDefinition = "text") private String paiDetails;
    @Column(name = "dietary_restrictions", columnDefinition = "text") private String dietaryRestrictions;
    @Column(name = "doctor_name", length = 160) private String doctorName;
    @Column(name = "doctor_phone", length = 40) private String doctorPhone;
    @Column(name = "emergency_contact_name", length = 160) private String emergencyContactName;
    @Column(name = "emergency_contact_phone", length = 40) private String emergencyContactPhone;
    @Column(name = "emergency_contact_relation", length = 60) private String emergencyContactRelation;
    @Column(length = 160) private String insurance;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}
