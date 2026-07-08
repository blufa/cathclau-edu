package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.domain.PayslipLineType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payslip_lines")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class PayslipLineJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "payslip_id", nullable = false) private Long payslipId;
    @Column(nullable = false) private String label;
    @Enumerated(EnumType.STRING) @Column(name = "line_type", nullable = false, length = 16) private PayslipLineType lineType;
    @Column(precision = 15, scale = 2) private BigDecimal amount;
}
