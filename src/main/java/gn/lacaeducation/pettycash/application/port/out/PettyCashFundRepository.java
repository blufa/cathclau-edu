package gn.lacaeducation.pettycash.application.port.out;
import gn.lacaeducation.pettycash.domain.PettyCashFund;
import java.util.List;
import java.util.Optional;
public interface PettyCashFundRepository {
    PettyCashFund save(PettyCashFund f);
    Optional<PettyCashFund> findById(Long id);
    List<PettyCashFund> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
