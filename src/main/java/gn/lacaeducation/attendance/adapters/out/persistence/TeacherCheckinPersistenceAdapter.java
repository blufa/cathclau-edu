package gn.lacaeducation.attendance.adapters.out.persistence;

import gn.lacaeducation.attendance.application.port.out.TeacherCheckinRepository;
import gn.lacaeducation.attendance.domain.TeacherCheckin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class TeacherCheckinPersistenceAdapter implements TeacherCheckinRepository {
    private final TeacherCheckinJpaRepository jpa;

    @Override public TeacherCheckin save(TeacherCheckin c) { return toDomain(jpa.save(toEntity(c))); }

    @Override public Optional<TeacherCheckin> findByTeacherIdAndDateAndSlot(Long t, LocalDate d, Long slot) {
        return jpa.findByTeacherIdAndCheckinDateAndScheduleSlotId(t, d, slot).map(this::toDomain);
    }

    @Override public List<TeacherCheckin> findByTeacherIdAndDate(Long t, LocalDate d) {
        return jpa.findByTeacherIdAndCheckinDate(t, d).stream().map(this::toDomain).toList();
    }

    @Override public List<TeacherCheckin> findByTeacherIdAndDateBetween(Long t, LocalDate from, LocalDate to) {
        return jpa.findByTeacherIdAndCheckinDateBetween(t, from, to).stream().map(this::toDomain).toList();
    }

    private TeacherCheckin toDomain(TeacherCheckinJpaEntity e) {
        return TeacherCheckin.builder().id(e.getId()).tenantId(e.getTenantId()).teacherId(e.getTeacherId())
                .checkinDate(e.getCheckinDate()).scheduleSlotId(e.getScheduleSlotId()).sessionStart(e.getSessionStart())
                .sessionEnd(e.getSessionEnd()).checkInTime(e.getCheckInTime()).checkOutTime(e.getCheckOutTime())
                .lateMinutes(e.getLateMinutes()).earlyMinutes(e.getEarlyMinutes()).createdAt(e.getCreatedAt()).build();
    }

    private TeacherCheckinJpaEntity toEntity(TeacherCheckin c) {
        TeacherCheckinJpaEntity e = new TeacherCheckinJpaEntity();
        e.setId(c.getId()); e.setTenantId(c.getTenantId()); e.setTeacherId(c.getTeacherId());
        e.setCheckinDate(c.getCheckinDate()); e.setScheduleSlotId(c.getScheduleSlotId()); e.setSessionStart(c.getSessionStart());
        e.setSessionEnd(c.getSessionEnd());
        e.setCheckInTime(c.getCheckInTime()); e.setCheckOutTime(c.getCheckOutTime());
        e.setLateMinutes(c.getLateMinutes() != null ? c.getLateMinutes() : 0);
        e.setEarlyMinutes(c.getEarlyMinutes() != null ? c.getEarlyMinutes() : 0);
        e.setCreatedAt(c.getCreatedAt());
        return e;
    }
}
