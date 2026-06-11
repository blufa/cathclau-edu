package gn.lacaeducation.payments;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PaymentPublicInfo(Long id, String reference, BigDecimal amount, String channel, LocalDateTime paidAt) {}
