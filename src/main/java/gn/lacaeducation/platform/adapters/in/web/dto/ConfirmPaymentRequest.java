package gn.lacaeducation.platform.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPaymentRequest(
    @NotBlank String externalTransactionId
) {}
