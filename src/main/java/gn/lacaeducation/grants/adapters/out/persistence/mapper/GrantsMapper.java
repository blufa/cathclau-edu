package gn.lacaeducation.grants.adapters.out.persistence.mapper;
import gn.lacaeducation.grants.adapters.out.persistence.GrantJpaEntity;
import gn.lacaeducation.grants.adapters.out.persistence.GrantPaymentJpaEntity;
import gn.lacaeducation.grants.domain.Grant;
import gn.lacaeducation.grants.domain.GrantPayment;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface GrantsMapper {
    Grant toDomain(GrantJpaEntity e);
    GrantJpaEntity toEntity(Grant d);
    GrantPayment toDomain(GrantPaymentJpaEntity e);
    GrantPaymentJpaEntity toEntity(GrantPayment d);
}
