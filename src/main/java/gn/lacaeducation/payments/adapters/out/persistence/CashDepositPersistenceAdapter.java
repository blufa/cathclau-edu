package gn.lacaeducation.payments.adapters.out.persistence;

import gn.lacaeducation.payments.application.port.out.CashDepositRepository;
import gn.lacaeducation.payments.domain.CashDeposit;
import gn.lacaeducation.payments.domain.CashDepositStatus;
import gn.lacaeducation.payments.domain.PaymentChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component @RequiredArgsConstructor
class CashDepositPersistenceAdapter implements CashDepositRepository {
    private final CashDepositJpaRepository jpa;

    @Override public CashDeposit save(CashDeposit d) { return toDomain(jpa.save(toEntity(d))); }
    @Override public Optional<CashDeposit> findById(Long id) { return jpa.findById(id).map(CashDepositPersistenceAdapter::toDomain); }
    @Override public Optional<CashDeposit> lockById(Long id) { return jpa.lockById(id).map(CashDepositPersistenceAdapter::toDomain); }
    @Override public Optional<CashDeposit> findByTenantIdAndIdempotencyKey(Long tenantId, String key) {
        return jpa.findByTenantIdAndIdempotencyKey(tenantId, key).map(CashDepositPersistenceAdapter::toDomain);
    }

    @Override public List<CashDeposit> search(Long tenantId, Long accountantId, CashDepositStatus status,
                                               PaymentChannel method, LocalDateTime from, LocalDateTime to) {
        return jpa.search(tenantId, accountantId, status, method, from, to).stream()
                .map(CashDepositPersistenceAdapter::toDomain).toList();
    }

    private static CashDepositJpaEntity toEntity(CashDeposit d) {
        CashDepositJpaEntity e = new CashDepositJpaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setAccountantId(d.getAccountantId());
        e.setAmount(d.getAmount());
        e.setCurrency(d.getCurrency());
        e.setMethod(d.getMethod());
        e.setDestinationAccount(d.getDestinationAccount());
        e.setDepositedAt(d.getDepositedAt());
        e.setTransactionReference(d.getTransactionReference());
        e.setComment(d.getComment());
        e.setStatus(d.getStatus());
        e.setValidatedBy(d.getValidatedBy());
        e.setValidatedAt(d.getValidatedAt());
        e.setRejectionReason(d.getRejectionReason());
        e.setIdempotencyKey(d.getIdempotencyKey());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    private static CashDeposit toDomain(CashDepositJpaEntity e) {
        return CashDeposit.builder()
                .id(e.getId()).tenantId(e.getTenantId()).accountantId(e.getAccountantId())
                .amount(e.getAmount()).currency(e.getCurrency()).method(e.getMethod())
                .destinationAccount(e.getDestinationAccount()).depositedAt(e.getDepositedAt())
                .transactionReference(e.getTransactionReference()).comment(e.getComment())
                .status(e.getStatus()).validatedBy(e.getValidatedBy()).validatedAt(e.getValidatedAt())
                .rejectionReason(e.getRejectionReason()).idempotencyKey(e.getIdempotencyKey())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
