package gn.lacaeducation.staff.adapters.out.persistence;

import gn.lacaeducation.staff.domain.EmployeeCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "employees")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class EmployeeJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "user_id") private Long userId;
    @Column(name = "employee_number") private String employeeNumber;
    @Column(name = "first_name") private String firstName;
    @Column(name = "last_name") private String lastName;
    private String phone;
    private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private EmployeeCategory category;
    @Column(name = "job_title") private String jobTitle;
    @Column(name = "hire_date") private LocalDate hireDate;
    @Column(name = "base_salary", precision = 15, scale = 2) private BigDecimal baseSalary;
    @Column(name = "cnss_number") private String cnssNumber;
    @Column(nullable = false) private Boolean active = true;
}
