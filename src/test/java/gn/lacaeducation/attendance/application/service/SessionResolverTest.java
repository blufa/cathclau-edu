package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.attendance.domain.RecessWindow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionResolverTest {

    private final SessionResolver resolver = new SessionResolver();

    private static ScheduleSlotPublicInfo slot(long id, String start, String end) {
        return new ScheduleSlotPublicInfo(id, 1L, 2L, 3L, 4L, "LUNDI", start, end, null);
    }

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

    // ── currentBlock : fusion des créneaux consécutifs en une seule séance de présence ──────────────

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 21); // un lundi

    @Test
    void currentBlock_threeBackToBackSlots_mergeIntoOneBlock() {
        // 8h-10h, 10h-12h, 12h-14h : un seul scan d'entrée/sortie doit couvrir toute la plage 8h-14h.
        List<ScheduleSlotPublicInfo> slots = List.of(
                slot(1, "08:00", "10:00"), slot(2, "10:00", "12:00"), slot(3, "12:00", "14:00"));

        SessionResolver.Block block = resolver.currentBlock(slots, MONDAY, LocalTime.of(11, 0), 5);

        assertThat(block.firstSlotId()).isEqualTo(1L);
        assertThat(block.start()).isEqualTo(LocalTime.of(8, 0));
        assertThat(block.end()).isEqualTo(LocalTime.of(14, 0));
        assertThat(block.slots()).hasSize(3);
    }

    @Test
    void currentBlock_gapLargerThanMargin_splitsIntoSeparateBlocks() {
        // 8h-10h puis 13h-15h (3h d'écart, marge de 5 min) : deux séances distinctes, pas fusionnées.
        List<ScheduleSlotPublicInfo> slots = List.of(slot(1, "08:00", "10:00"), slot(2, "13:00", "15:00"));

        SessionResolver.Block morning = resolver.currentBlock(slots, MONDAY, LocalTime.of(9, 0), 5);
        SessionResolver.Block afternoon = resolver.currentBlock(slots, MONDAY, LocalTime.of(14, 0), 5);

        assertThat(morning.firstSlotId()).isEqualTo(1L);
        assertThat(morning.end()).isEqualTo(LocalTime.of(10, 0));
        assertThat(afternoon.firstSlotId()).isEqualTo(2L);
        assertThat(afternoon.start()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    void currentBlock_gapWithinMargin_stillMerges() {
        // 8h-10h puis 10h05-12h (5 min d'écart, marge de 5 min) : toujours une seule séance continue —
        // le temps de changer de salle ne doit pas exiger un nouveau scan.
        List<ScheduleSlotPublicInfo> slots = List.of(slot(1, "08:00", "10:00"), slot(2, "10:05", "12:00"));

        SessionResolver.Block block = resolver.currentBlock(slots, MONDAY, LocalTime.of(10, 2), 5);

        assertThat(block.firstSlotId()).isEqualTo(1L);
        assertThat(block.end()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void currentBlock_noSlotsThatDay_returnsNull() {
        SessionResolver.Block block = resolver.currentBlock(List.of(slot(1, "08:00", "10:00")),
                LocalDate.of(2026, 9, 22), LocalTime.of(9, 0), 5); // mardi, aucun créneau ce jour-là

        assertThat(block).isNull();
    }

    @Test
    void currentBlock_afterAllBlocksEnded_returnsLastBlock() {
        List<ScheduleSlotPublicInfo> slots = List.of(slot(1, "08:00", "10:00"), slot(2, "10:00", "12:00"));

        SessionResolver.Block block = resolver.currentBlock(slots, MONDAY, LocalTime.of(18, 0), 5);

        assertThat(block.firstSlotId()).isEqualTo(1L);
        assertThat(block.end()).isEqualTo(LocalTime.of(12, 0));
    }
}
