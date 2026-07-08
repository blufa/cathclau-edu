package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.domain.PayslipStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payslips")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class PayslipJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Enumerated(EnumType.STRING) @Column(name = "employee_type", nullable = false, length = 16)
    private gn.lacaeducation.payroll.domain.EmployeeType employeeType = gn.lacaeducation.payroll.domain.EmployeeType.TEACHER;
    @Column(name = "teacher_id") private Long teacherId;
    @Column(name = "teacher_name") private String teacherName;
    @Column(name = "period_label", nullable = false, length = 64) private String periodLabel;
    @Column(name = "period_start") private LocalDate periodStart;
    @Column(name = "period_end") private LocalDate periodEnd;
    @Column(name = "base_salary", precision = 15, scale = 2) private BigDecimal baseSalary;
    @Column(name = "gross_salary", precision = 15, scale = 2) private BigDecimal grossSalary;
    @Column(name = "net_salary", precision = 15, scale = 2) private BigDecimal netSalary;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private PayslipStatus status;
    @Column(name = "payment_method", length = 24) private String paymentMethod;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "paid_at") private LocalDateTime paidAt;
}
