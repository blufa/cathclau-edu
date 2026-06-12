package gn.lacaeducation.platform.domain;

import gn.lacaeducation.common.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionStateMachineTest {

    private Subscription withStatus(SubscriptionStatus status) {
        return Subscription.builder().id(1L).tenantId(1L).status(status).build();
    }

    @Test
    void pendingToActive_allowed() {
        Subscription s = withStatus(SubscriptionStatus.PENDING);
        s.transitionTo(SubscriptionStatus.ACTIVE);
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void activeToSuspendedExpiredCancelled_allowed() {
        assertThat(withStatus(SubscriptionStatus.ACTIVE).canTransitionTo(SubscriptionStatus.SUSPENDED)).isTrue();
        assertThat(withStatus(SubscriptionStatus.ACTIVE).canTransitionTo(SubscriptionStatus.EXPIRED)).isTrue();
        assertThat(withStatus(SubscriptionStatus.ACTIVE).canTransitionTo(SubscriptionStatus.CANCELLED)).isTrue();
    }

    @Test
    void pendingToSuspended_illegal() {
        Subscription s = withStatus(SubscriptionStatus.PENDING);
        assertThatThrownBy(() -> s.transitionTo(SubscriptionStatus.SUSPENDED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancelledIsTerminal() {
        Subscription s = withStatus(SubscriptionStatus.CANCELLED);
        assertThatThrownBy(() -> s.transitionTo(SubscriptionStatus.ACTIVE))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(s.canTransitionTo(SubscriptionStatus.SUSPENDED)).isFalse();
    }

    @Test
    void sameStatusIsNoOp() {
        Subscription s = withStatus(SubscriptionStatus.ACTIVE);
        s.transitionTo(SubscriptionStatus.ACTIVE);
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void renewalActiveToPending_allowed() {
        Subscription s = withStatus(SubscriptionStatus.ACTIVE);
        s.transitionTo(SubscriptionStatus.PENDING);
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
    }
}
