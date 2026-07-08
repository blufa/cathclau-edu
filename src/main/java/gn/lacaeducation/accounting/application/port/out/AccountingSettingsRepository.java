package gn.lacaeducation.accounting.application.port.out;
import gn.lacaeducation.accounting.domain.AccountingSettings;
import java.util.Optional;
public interface AccountingSettingsRepository {
    AccountingSettings save(AccountingSettings s);
    Optional<AccountingSettings> findByTenantId(Long tenantId);
}
