package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.SalaryAdvanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_advances")
@Getter
@Setter
@NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class SalaryAdvanceJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Enumerated(EnumType.STRING) @Column(name = "employee_type", nullable = false, length = 16)
    private EmployeeType employeeType;
    @Column(name = "teacher_id", nullable = false) private Long teacherId;
    @Column(name = "teacher_name") private String teacherName;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(name = "remaining_balance", nullable = false, precision = 15, scale = 2) private BigDecimal remainingBalance;
    @Column(columnDefinition = "text") private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private SalaryAdvanceStatus status;
    @Column(name = "granted_at") private LocalDateTime grantedAt;
    @Column(name = "last_deducted_at") private LocalDateTime lastDeductedAt;
    @Column(name = "granted_by_user_id") private Long grantedByUserId;
}
