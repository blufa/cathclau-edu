package gn.lacaeducation.pettycash.application.port.out;
import gn.lacaeducation.pettycash.domain.PettyCashTransaction;
import java.util.List;
import java.util.Optional;
public interface PettyCashTransactionRepository {
    PettyCashTransaction save(PettyCashTransaction t);
    Optional<PettyCashTransaction> findById(Long id);
    List<PettyCashTransaction> findByFundId(Long fundId);
    void deleteById(Long id);
    void deleteByFundId(Long fundId);
}
