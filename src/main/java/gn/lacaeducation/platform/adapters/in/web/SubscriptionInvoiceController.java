package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.adapters.in.web.dto.SubscriptionInvoiceResponse;
import gn.lacaeducation.platform.application.port.in.InvoiceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Subscription Invoices", description = "Editor-level subscription invoices (SUPER_ADMIN)")
@RestController
@RequestMapping("/api/v1/platform/subscription-invoices")
@RequiredArgsConstructor
public class SubscriptionInvoiceController {

    private final InvoiceUseCase invoiceUseCase;

    @Operation(summary = "List subscription invoices (filter: status)")
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<SubscriptionInvoiceResponse> findAll(@RequestParam(required = false) String status) {
        return invoiceUseCase.findAllInvoices(status).stream()
                .map(SubscriptionInvoiceResponse::from).toList();
    }

    @Operation(summary = "Get a subscription invoice by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SubscriptionInvoiceResponse findById(@PathVariable Long id) {
        return SubscriptionInvoiceResponse.from(invoiceUseCase.findInvoiceById(id));
    }

    @Operation(summary = "Download a subscription invoice PDF (URL)")
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Map<String, String> pdf(@PathVariable Long id) {
        return Map.of("pdfUrl", invoiceUseCase.generateInvoicePdf(id));
    }

    @Operation(summary = "Resend (relancer) a subscription invoice reminder")
    @PostMapping("/{id}/resend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SubscriptionInvoiceResponse resend(@PathVariable Long id) {
        return SubscriptionInvoiceResponse.from(invoiceUseCase.resendInvoice(id));
    }
}
