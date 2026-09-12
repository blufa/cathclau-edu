package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.attendance.domain.RecessWindow;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionResolverTest {

    private final SessionResolver resolver = new SessionResolver();

    @Test
    void graceDeadline_withoutRecess_addsMarginToStart() {
        LocalTime deadline = resolver.graceDeadline(LocalTime.of(14, 0), 5, List.of());

        assertThat(deadline).isEqualTo(LocalTime.of(14, 5));
    }

    @Test
    void graceDeadline_zeroMargin_equalsStart() {
        LocalTime deadline = resolver.graceDeadline(LocalTime.of(8, 0), 0, List.of());

        assertThat(deadline).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void graceDeadline_startInsideRecessWindow_pushedToRecessEnd() {
        RecessWindow recess = RecessWindow.builder().startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 15)).build();

        LocalTime deadline = resolver.graceDeadline(LocalTime.of(10, 5), 5, List.of(recess));

        // La récréation (10h15) dépasse le début + marge (10h10) : c'est elle qui prévaut.
        assertThat(deadline).isEqualTo(LocalTime.of(10, 15));
    }

    @Test
    void graceDeadline_marginLongerThanRecess_marginPrevails() {
        RecessWindow recess = RecessWindow.builder().startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 5)).build();

        LocalTime deadline = resolver.graceDeadline(LocalTime.of(10, 0), 10, List.of(recess));

        // Début + marge (10h10) dépasse la fin de récréation (10h05) : c'est la marge qui prévaut.
        assertThat(deadline).isEqualTo(LocalTime.of(10, 10));
    }
}
