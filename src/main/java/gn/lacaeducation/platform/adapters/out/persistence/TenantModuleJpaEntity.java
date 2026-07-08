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

import java.time.LocalDate;

@Entity
@Table(name = "tenant_module", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "module_code"}))
@Getter
@Setter
@NoArgsConstructor
public class TenantModuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", nullable = false, length = 32)
    private ModuleCode moduleCode;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;
}
