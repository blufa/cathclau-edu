package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.PaymentChannel;
import gn.lacaeducation.platform.domain.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record InitiatePaymentRequest(
    @NotNull Long invoiceId,
    @NotNull @Positive BigDecimal amount,
    @NotNull PaymentChannel channel,
    @NotNull PaymentType paymentType
) {}
