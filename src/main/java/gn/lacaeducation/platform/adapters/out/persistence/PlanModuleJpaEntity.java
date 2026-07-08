package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.ModuleCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plan_module", uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "module_code"}))
@Getter
@Setter
@NoArgsConstructor
public class PlanModuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", nullable = false, length = 32)
    private ModuleCode moduleCode;

    @Column(nullable = false)
    private boolean active = true;
}
