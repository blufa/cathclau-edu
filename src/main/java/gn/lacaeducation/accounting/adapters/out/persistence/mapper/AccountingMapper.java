package gn.lacaeducation.accounting.adapters.out.persistence.mapper;

import gn.lacaeducation.accounting.adapters.out.persistence.*;
import gn.lacaeducation.accounting.domain.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountingMapper {
    AccountingSettings toDomain(AccountingSettingsJpaEntity e);
    AccountingSettingsJpaEntity toEntity(AccountingSettings d);

    FiscalYear toDomain(FiscalYearJpaEntity e);
    FiscalYearJpaEntity toEntity(FiscalYear d);

    TreasuryAccount toDomain(TreasuryAccountJpaEntity e);
    TreasuryAccountJpaEntity toEntity(TreasuryAccount d);

    CashFlowEntry toDomain(CashFlowEntryJpaEntity e);
    CashFlowEntryJpaEntity toEntity(CashFlowEntry d);

    ChartOfAccount toDomain(ChartOfAccountJpaEntity e);
    ChartOfAccountJpaEntity toEntity(ChartOfAccount d);

    Journal toDomain(JournalJpaEntity e);
    JournalJpaEntity toEntity(Journal d);

    JournalEntryLine toDomain(JournalEntryLineJpaEntity e);
    JournalEntryLineJpaEntity toEntity(JournalEntryLine d);

    JournalEntry toDomain(JournalEntryJpaEntity e);
    JournalEntryJpaEntity toEntity(JournalEntry d);

    BankReconciliation toDomain(BankReconciliationJpaEntity e);
    BankReconciliationJpaEntity toEntity(BankReconciliation d);

    ReconciliationItem toDomain(ReconciliationItemJpaEntity e);
    ReconciliationItemJpaEntity toEntity(ReconciliationItem d);
}
