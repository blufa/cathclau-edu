package gn.lacaeducation.platform.adapters.out.persistence.mapper;

import gn.lacaeducation.platform.adapters.out.persistence.PlanChangeJpaEntity;
import gn.lacaeducation.platform.adapters.out.persistence.PlanJpaEntity;
import gn.lacaeducation.platform.adapters.out.persistence.SubscriptionInvoiceJpaEntity;
import gn.lacaeducation.platform.adapters.out.persistence.SubscriptionJpaEntity;
import gn.lacaeducation.platform.adapters.out.persistence.SubscriptionPaymentJpaEntity;
import gn.lacaeducation.platform.adapters.out.persistence.SubscriptionReceiptJpaEntity;
import gn.lacaeducation.platform.adapters.out.persistence.EmailVerificationTokenJpaEntity;
import gn.lacaeducation.platform.adapters.out.persistence.PlatformFeeLedgerEntryJpaEntity;
import gn.lacaeducation.platform.adapters.out.persistence.PlatformFeeSettingJpaEntity;
import gn.lacaeducation.platform.adapters.out.persistence.PlatformFeeSettlementJpaEntity;
import gn.lacaeducation.platform.adapters.out.persistence.PrefectureJpaEntity;
import gn.lacaeducation.platform.adapters.out.persistence.TenantJpaEntity;
import gn.lacaeducation.platform.domain.EmailVerificationToken;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanChange;
import gn.lacaeducation.platform.domain.PlatformFeeLedgerEntry;
import gn.lacaeducation.platform.domain.PlatformFeeSetting;
import gn.lacaeducation.platform.domain.PlatformFeeSettlement;
import gn.lacaeducation.platform.domain.Prefecture;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.SubscriptionInvoice;
import gn.lacaeducation.platform.domain.SubscriptionPayment;
import gn.lacaeducation.platform.domain.SubscriptionReceipt;
import gn.lacaeducation.platform.domain.Tenant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlatformMapper {

    Tenant toDomain(TenantJpaEntity entity);

    TenantJpaEntity toEntity(Tenant domain);

    Plan toDomain(PlanJpaEntity entity);

    PlanJpaEntity toEntity(Plan domain);

    Subscription toDomain(SubscriptionJpaEntity entity);

    SubscriptionJpaEntity toEntity(Subscription domain);

    SubscriptionInvoice toDomain(SubscriptionInvoiceJpaEntity entity);

    SubscriptionInvoiceJpaEntity toEntity(SubscriptionInvoice domain);

    SubscriptionPayment toDomain(SubscriptionPaymentJpaEntity entity);

    SubscriptionPaymentJpaEntity toEntity(SubscriptionPayment domain);

    SubscriptionReceipt toDomain(SubscriptionReceiptJpaEntity entity);

    SubscriptionReceiptJpaEntity toEntity(SubscriptionReceipt domain);

    PlanChange toDomain(PlanChangeJpaEntity entity);

    PlanChangeJpaEntity toEntity(PlanChange domain);

    EmailVerificationToken toDomain(EmailVerificationTokenJpaEntity entity);

    EmailVerificationTokenJpaEntity toEntity(EmailVerificationToken domain);

    PlatformFeeSetting toDomain(PlatformFeeSettingJpaEntity entity);

    PlatformFeeSettingJpaEntity toEntity(PlatformFeeSetting domain);

    PlatformFeeLedgerEntry toDomain(PlatformFeeLedgerEntryJpaEntity entity);

    PlatformFeeLedgerEntryJpaEntity toEntity(PlatformFeeLedgerEntry domain);

    PlatformFeeSettlement toDomain(PlatformFeeSettlementJpaEntity entity);

    PlatformFeeSettlementJpaEntity toEntity(PlatformFeeSettlement domain);

    Prefecture toDomain(PrefectureJpaEntity entity);

    PrefectureJpaEntity toEntity(Prefecture domain);
}
