package gn.lacaeducation.staff.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherWorkloadTest {

    @Test
    void from_sumsDurationsAndGroupsByDay() {
        List<TeacherWorkload.Slot> slots = List.of(
                new TeacherWorkload.Slot("LUNDI", LocalTime.of(8, 0), LocalTime.of(10, 0)),   // 120 min
                new TeacherWorkload.Slot("LUNDI", LocalTime.of(10, 0), LocalTime.of(11, 0)),  // 60 min
                new TeacherWorkload.Slot("MARDI", LocalTime.of(8, 0), LocalTime.of(9, 30)));  // 90 min

        TeacherWorkload w = TeacherWorkload.from(42L, slots);

        assertThat(w.teacherId()).isEqualTo(42L);
        assertThat(w.totalWeeklyMinutes()).isEqualTo(270);
        assertThat(w.totalWeeklyHours()).isEqualByComparingTo("4.50");
        assertThat(w.slotCount()).isEqualTo(3);
        assertThat(w.minutesByDay()).containsEntry("LUNDI", 180).containsEntry("MARDI", 90);
    }

    @Test
    void from_ignoresIncoherentOrIncompleteSlots() {
        List<TeacherWorkload.Slot> slots = List.of(
                new TeacherWorkload.Slot("LUNDI", LocalTime.of(10, 0), LocalTime.of(9, 0)),  // end<=start
                new TeacherWorkload.Slot("MARDI", null, LocalTime.of(9, 0)),                 // borne absente
                new TeacherWorkload.Slot("MERCREDI", LocalTime.of(8, 0), LocalTime.of(9, 0))); // 60 min ok

        TeacherWorkload w = TeacherWorkload.from(1L, slots);

        assertThat(w.slotCount()).isEqualTo(1);
        assertThat(w.totalWeeklyMinutes()).isEqualTo(60);
        assertThat(w.minutesByDay()).containsOnlyKeys("MERCREDI");
    }

    @Test
    void from_emptySchedule_isZero() {
        TeacherWorkload w = TeacherWorkload.from(1L, List.of());
        assertThat(w.totalWeeklyMinutes()).isZero();
        assertThat(w.totalWeeklyHours()).isEqualByComparingTo("0.00");
        assertThat(w.minutesByDay()).isEmpty();
    }
}
