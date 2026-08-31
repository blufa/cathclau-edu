package gn.lacaeducation.staff.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "teachers")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class EnseignantJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "user_id") private Long userId;
    @Column(name = "staff_number") private String staffNumber;
    @Column(name = "first_name") private String firstName;
    @Column(name = "last_name") private String lastName;
    private String phone;
    private String email;
    private String photo;
    private String specialization;
    @Column(name = "hire_date") private LocalDate hireDate;
    @Column(nullable = false) private Boolean active = true;
    @Column(name = "employment_status") private String employmentStatus;
    private String diploma;
    @Column(name = "remuneration_type") private String remunerationType;
    @Column(name = "remuneration_rate", precision = 12, scale = 2) private BigDecimal remunerationRate;
    @Column(name = "teaching_authorization_number") private String teachingAuthorizationNumber;
    @Column(name = "cnss_number") private String cnssNumber;
}
