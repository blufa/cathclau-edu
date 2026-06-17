package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * All Djomy payment methods (per the partner OpenAPI spec). Two distinct vocabularies exist:
 *
 *  - {@link #payerMethods()} — single method codes accepted by the direct {@code POST /v1/payments}
 *    endpoint (OM, MOMO, KULU, …).
 *  - {@link #portalMethods()} — the {@code allowedPaymentMethods} codes shown on the redirect
 *    portal ({@code POST /v1/payments/gateway}); note cards are grouped under the single
 *    {@code CARD} code there, whereas a paid transaction reports VISA/MC/AMEX individually.
 */
public enum DjomyPaymentMethod {
    OM(true, true),            // Orange Money
    MOMO(true, true),          // MTN Mobile Money
    KULU(true, false),         // Kulu (Digital Pay) — redirect link on the direct endpoint
    SOUTRA_MONEY(true, true),  // Soutra Money (bientôt)
    PAYCARD(true, true),       // PayCard (bientôt)
    YMO(true, false),          // Ymo (bientôt)
    CARD(false, true),         // Portal-only umbrella for VISA & MasterCard
    VISA(false, false),        // Reported on captured card payments
    MC(false, false),          // MasterCard
    AMEX(false, false);        // American Express (bientôt)

    private final boolean payer;
    private final boolean portal;

    DjomyPaymentMethod(boolean payer, boolean portal) {
        this.payer = payer;
        this.portal = portal;
    }

    /** Method codes selectable on the Djomy redirect portal ({@code allowedPaymentMethods}). */
    public static List<String> portalMethods() {
        return Arrays.stream(values()).filter(m -> m.portal).map(Enum::name).collect(Collectors.toList());
    }

    /** Method codes accepted as a single {@code paymentMethod} on the direct payment endpoint. */
    public static List<String> payerMethods() {
        return Arrays.stream(values()).filter(m -> m.payer).map(Enum::name).collect(Collectors.toList());
    }
}
