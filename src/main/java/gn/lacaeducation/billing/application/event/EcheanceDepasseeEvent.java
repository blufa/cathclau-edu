package gn.lacaeducation.billing.application.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EcheanceDepasseeEvent(Long installmentId, Long feeId, BigDecimal amount, LocalDate dueDate) {}
