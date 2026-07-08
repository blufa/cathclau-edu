package gn.lacaeducation.pettycash.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface PettyCashTransactionJpaRepository extends JpaRepository<PettyCashTransactionJpaEntity, Long> {
    List<PettyCashTransactionJpaEntity> findByFundIdOrderByTxDateDescIdDesc(Long fundId);
    void deleteByFundId(Long fundId);
}
