package gn.lacaeducation.payroll.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "salary_structures")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class SalaryStructureJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false, length = 128) private String name;
    @Column(length = 32) private String code;
    @Column(nullable = false) private boolean active;
}
