package gn.lacaeducation.payments.adapters.out.persistence;
import gn.lacaeducation.payments.domain.*;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
interface PaymentsMapper {
    Payment toDomain(PaymentJpaEntity e);
    PaymentJpaEntity toEntity(Payment d);
    PaymentTransaction toDomain(PaymentTransactionJpaEntity e);
    PaymentTransactionJpaEntity toEntity(PaymentTransaction d);
    Receipt toDomain(ReceiptJpaEntity e);
    ReceiptJpaEntity toEntity(Receipt d);
}
