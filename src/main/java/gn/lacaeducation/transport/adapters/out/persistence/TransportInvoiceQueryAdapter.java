package gn.lacaeducation.transport.adapters.out.persistence;

import gn.lacaeducation.transport.application.port.out.TransportInvoiceQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
class TransportInvoiceQueryAdapter implements TransportInvoiceQueryPort {

    private final TransportInvoiceMirrorJpaRepository repo;

    @Override
    public Map<Long, Long> countPaidTransportMonthsByStudentId(Long tenantId) {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (Object[] row : repo.countPaidTransportMonthsByStudentId(tenantId)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }
}
