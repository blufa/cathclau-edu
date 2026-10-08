package gn.lacaeducation.transport.adapters.out.persistence;

import gn.lacaeducation.transport.application.port.out.TransportInvoiceQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class TransportInvoiceQueryAdapter implements TransportInvoiceQueryPort {

    private final TransportInvoiceMirrorJpaRepository repo;

    @Override
    public List<Long> findStudentIdsWithPaidTransportInvoice(Long tenantId) {
        return repo.findStudentIdsWithPaidTransportInvoice(tenantId);
    }
}
