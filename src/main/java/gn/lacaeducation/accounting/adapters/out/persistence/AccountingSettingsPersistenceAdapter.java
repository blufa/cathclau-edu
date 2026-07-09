package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.adapters.out.persistence.mapper.AccountingMapper;
import gn.lacaeducation.accounting.application.port.out.AccountingSettingsRepository;
import gn.lacaeducation.accounting.domain.AccountingSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component @RequiredArgsConstructor
class AccountingSettingsPersistenceAdapter implements AccountingSettingsRepository {
    private final AccountingSettingsJpaRepository jpa;
    private final AccountingMapper mapper;
    @Override public AccountingSettings save(AccountingSettings s) { return mapper.toDomain(jpa.save(mapper.toEntity(s))); }
    @Override public Optional<AccountingSettings> findByTenantId(Long t) { return jpa.findByTenantId(t).map(mapper::toDomain); }
}
