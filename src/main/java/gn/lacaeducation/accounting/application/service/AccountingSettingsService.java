package gn.lacaeducation.accounting.application.service;

import gn.lacaeducation.accounting.application.port.out.AccountingSettingsRepository;
import gn.lacaeducation.accounting.domain.AccountingMode;
import gn.lacaeducation.accounting.domain.AccountingSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Accounting settings per tenant (mode SIMPLE | DOUBLE_ENTRY). */
@Service
@Transactional
@RequiredArgsConstructor
public class AccountingSettingsService {

    private final AccountingSettingsRepository repo;

    @Transactional(readOnly = true)
    public AccountingSettings getOrDefault(Long tenantId) {
        return repo.findByTenantId(tenantId).orElseGet(() -> AccountingSettings.builder()
                .tenantId(tenantId).mode(AccountingMode.SIMPLE).referential("SYSCOHADA")
                .currency("GNF").fiscalYearStartMonth(9).vatApplicable(false)
                .defaultVatRate(BigDecimal.ZERO).autoNumbering(true).active(true).build());
    }

    public AccountingSettings update(Long tenantId, AccountingSettings patch) {
        AccountingSettings s = repo.findByTenantId(tenantId).orElseGet(() -> getOrDefault(tenantId));
        if (patch.getMode() != null) s.setMode(patch.getMode());
        if (patch.getReferential() != null) s.setReferential(patch.getReferential());
        if (patch.getCurrency() != null) s.setCurrency(patch.getCurrency());
        if (patch.getFiscalYearStartMonth() != null) s.setFiscalYearStartMonth(patch.getFiscalYearStartMonth());
        if (patch.getVatApplicable() != null) s.setVatApplicable(patch.getVatApplicable());
        if (patch.getDefaultVatRate() != null) s.setDefaultVatRate(patch.getDefaultVatRate());
        if (patch.getAutoNumbering() != null) s.setAutoNumbering(patch.getAutoNumbering());
        if (patch.getActive() != null) s.setActive(patch.getActive());
        return repo.save(s);
    }
}
