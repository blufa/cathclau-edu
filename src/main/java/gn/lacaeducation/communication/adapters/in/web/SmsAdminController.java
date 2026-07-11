package gn.lacaeducation.communication.adapters.in.web;

import gn.lacaeducation.communication.application.port.out.SmsBalancePort;
import gn.lacaeducation.communication.application.port.out.SmsBalancePort.SmsBalance;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administration SMS (SUPER_ADMIN) : solde du bundle Orange (global au compte plateforme).
 * Renvoie une liste vide si le fournisseur SMS n'est pas configuré.
 */
@Tag(name = "SMS Admin", description = "Solde et administration du fournisseur SMS")
@RestController
@RequestMapping("/api/v1/communications/sms")
@RequiredArgsConstructor
public class SmsAdminController {

    private final SmsBalancePort smsBalancePort;

    @Operation(summary = "Solde SMS du compte plateforme (un élément par pays)")
    @GetMapping("/solde")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<SmsBalance> balance() {
        return smsBalancePort.fetchBalances();
    }
}
