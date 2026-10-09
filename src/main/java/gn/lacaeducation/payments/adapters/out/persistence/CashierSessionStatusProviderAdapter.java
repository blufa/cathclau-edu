package gn.lacaeducation.payments.adapters.out.persistence;

import gn.lacaeducation.payments.CashierSessionStatusProvider;
import gn.lacaeducation.payments.application.port.out.CashierSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CashierSessionStatusProviderAdapter implements CashierSessionStatusProvider {

    private final CashierSessionRepository repository;

    @Override
    public boolean hasOpenSession(Long cashierId) {
        return cashierId != null && repository.findOpenByCashierId(cashierId).isPresent();
    }
}
