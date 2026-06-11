package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.domain.*;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
interface BillingMapper {
    SchoolFee toDomain(SchoolFeeJpaEntity e);
    SchoolFeeJpaEntity toEntity(SchoolFee d);
    Installment toDomain(InstallmentJpaEntity e);
    InstallmentJpaEntity toEntity(Installment d);
    Invoice toDomain(InvoiceJpaEntity e);
    InvoiceJpaEntity toEntity(Invoice d);
}
