package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.domain.CashFlowReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface CashFlowEntryJpaRepository extends JpaRepository<CashFlowEntryJpaEntity, Long> {
    List<CashFlowEntryJpaEntity> findByTenantId(Long tenantId);
    List<CashFlowEntryJpaEntity> findByAccountId(Long accountId);
    Optional<CashFlowEntryJpaEntity> findByReferenceTypeAndReferenceId(CashFlowReferenceType referenceType, Long referenceId);
}
