package gn.lacaeducation.accounting.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
interface AccountingSettingsJpaRepository extends JpaRepository<AccountingSettingsJpaEntity, Long> {
    Optional<AccountingSettingsJpaEntity> findByTenantId(Long tenantId);
}
