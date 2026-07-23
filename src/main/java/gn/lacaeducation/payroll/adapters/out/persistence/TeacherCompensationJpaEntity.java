package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.domain.PayMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity @Table(name = "teacher_compensation")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class TeacherCompensationJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "teacher_id", nullable = false) private Long teacherId;
    @Enumerated(EnumType.STRING) @Column(name = "pay_mode", nullable = false, length = 16) private PayMode payMode;
    @Column(name = "hourly_rate_normal", nullable = false, precision = 15, scale = 2) private BigDecimal hourlyRateNormal;
    @Column(name = "hourly_rate_exam", nullable = false, precision = 15, scale = 2) private BigDecimal hourlyRateExam;
    @Column(name = "fixed_salary", nullable = false, precision = 15, scale = 2) private BigDecimal fixedSalary;
}
