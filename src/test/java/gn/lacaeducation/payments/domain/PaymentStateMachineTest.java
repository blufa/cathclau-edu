package gn.lacaeducation.payments.domain;

import gn.lacaeducation.common.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStateMachineTest {

    private Payment withStatus(PaymentStatus s) {
        return Payment.builder().id(1L).status(s).build();
    }

    @Test
    void legalFlow_initiatedToPendingToSucceededToRefunded() {
        Payment p = withStatus(PaymentStatus.INITIATED);
        p.transitionTo(PaymentStatus.PENDING);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING);
        p.transitionTo(PaymentStatus.SUCCEEDED);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        p.transitionTo(PaymentStatus.REFUNDED);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void pendingCanFail() {
        Payment p = withStatus(PaymentStatus.PENDING);
        p.transitionTo(PaymentStatus.FAILED);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void cannotSucceedDirectlyFromInitiated() {
        assertThatThrownBy(() -> withStatus(PaymentStatus.INITIATED).transitionTo(PaymentStatus.SUCCEEDED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cannotRefundAFailedPayment() {
        assertThatThrownBy(() -> withStatus(PaymentStatus.FAILED).transitionTo(PaymentStatus.REFUNDED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void terminalStatesRejectFurtherTransitions() {
        assertThatThrownBy(() -> withStatus(PaymentStatus.REFUNDED).transitionTo(PaymentStatus.SUCCEEDED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void sameStatusIsNoOp() {
        Payment p = withStatus(PaymentStatus.PENDING);
        p.transitionTo(PaymentStatus.PENDING);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }
}
