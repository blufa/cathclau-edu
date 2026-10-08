package gn.lacaeducation.billing.adapters.in.web;

import gn.lacaeducation.billing.application.port.in.ClassTransferRefundUseCase;
import gn.lacaeducation.billing.domain.ClassTransferRefund;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Surplus de crédit laissés par un transfert de classe (§retour comptable) — le parent avait déjà
 * réglé plus que ce que coûtent les mensualités de la nouvelle classe. Visible sur le reçu
 * (§OpenPdfReceiptGenerator) tant qu'il n'est pas marqué remboursé ici.
 */
@Tag(name = "Class transfer refunds", description = "Surplus à rembourser après un transfert de classe")
@RestController
@RequestMapping("/api/v1/billing/class-transfer-refunds")
@RequiredArgsConstructor
public class ClassTransferRefundController {

    private final ClassTransferRefundUseCase useCase;

    @Operation(summary = "Historique des surplus (remboursés ou non) d'un élève")
    @PreAuthorize("hasAuthority('facture:read')")
    @GetMapping("/student/{studentId}")
    public List<ClassTransferRefund> findByStudent(@PathVariable Long studentId) {
        return useCase.findByStudentId(studentId);
    }

    @Operation(summary = "Confirmer que le surplus a été physiquement remboursé au parent")
    @PreAuthorize("hasAuthority('facture:update')")
    @PatchMapping("/{id}/rembourse")
    public ClassTransferRefund markRefunded(@PathVariable Long id) {
        return useCase.markRefunded(id);
    }
}
