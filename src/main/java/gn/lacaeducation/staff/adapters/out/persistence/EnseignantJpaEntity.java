package gn.lacaeducation.staff.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    private String specialization;
    @Column(name = "hire_date") private LocalDate hireDate;
    @Column(nullable = false) private Boolean active = true;
}
