package gn.lacaeducation.billing;
import java.math.BigDecimal;
import java.time.LocalDate;
public record InvoicePublicInfo(Long id, String reference, BigDecimal totalAmount, BigDecimal remainingBalance, LocalDate issueDate) {}
