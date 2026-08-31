package gn.lacaeducation.platform;

import java.math.BigDecimal;

/**
 * Published API (consumed by the {@code billing} module) exposing the platform usage fee amount
 * currently configured by SUPER_ADMIN. Kept a single method so {@code billing} never needs to know
 * about the settings history/versioning — only the amount to apply to invoices generated right now.
 */
public interface PlatformFeeQuoteApi {

    /** Currently active platform fee amount, or {@link BigDecimal#ZERO} if none is configured. */
    BigDecimal currentAmount();
}
