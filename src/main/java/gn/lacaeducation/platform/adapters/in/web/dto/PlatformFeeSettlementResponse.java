package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.PaymentChannel;
import gn.lacaeducation.platform.domain.PaymentStatus;
import gn.lacaeducation.platform.domain.PlatformFeeSettlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlatformFeeSettlementResponse(
        Long id,
        String reference,
        BigDecimal amount,
        PaymentChannel channel,
        PaymentStatus status,
        String redirectUrl,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt
) {
    public static PlatformFeeSettlementResponse from(PlatformFeeSettlement s) {
        return new PlatformFeeSettlementResponse(s.getId(), s.getReference(), s.getAmount(), s.getChannel(),
                s.getStatus(), s.getRedirectUrl(), s.getCreatedAt(), s.getConfirmedAt());
    }
}
