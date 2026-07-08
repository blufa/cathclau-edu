package gn.lacaeducation.pettycash.adapters.out.persistence.mapper;
import gn.lacaeducation.pettycash.adapters.out.persistence.PettyCashFundJpaEntity;
import gn.lacaeducation.pettycash.adapters.out.persistence.PettyCashTransactionJpaEntity;
import gn.lacaeducation.pettycash.domain.PettyCashFund;
import gn.lacaeducation.pettycash.domain.PettyCashTransaction;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface PettyCashMapper {
    PettyCashFund toDomain(PettyCashFundJpaEntity e);
    PettyCashFundJpaEntity toEntity(PettyCashFund d);
    PettyCashTransaction toDomain(PettyCashTransactionJpaEntity e);
    PettyCashTransactionJpaEntity toEntity(PettyCashTransaction d);
}
