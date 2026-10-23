package gn.lacaeducation.attendance.adapters.out.persistence;

import gn.lacaeducation.attendance.application.port.out.StudentCheckinRepository;
import gn.lacaeducation.attendance.domain.StudentCheckin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class StudentCheckinPersistenceAdapter implements StudentCheckinRepository {
    private final StudentCheckinJpaRepository jpa;

    @Override public StudentCheckin save(StudentCheckin c) { return toDomain(jpa.save(toEntity(c))); }

    @Override public Optional<StudentCheckin> findByStudentIdAndDateAndSlot(Long s, LocalDate d, Long slot) {
        return jpa.findByStudentIdAndCheckinDateAndScheduleSlotId(s, d, slot).map(this::toDomain);
    }

    @Override public Optional<StudentCheckin> lockByStudentIdAndDateAndSlot(Long s, LocalDate d, Long slot) {
        return jpa.lockByStudentIdAndCheckinDateAndScheduleSlotId(s, d, slot).map(this::toDomain);
    }

    @Override public Optional<StudentCheckin> lockOpenByStudentIdAndDate(Long s, LocalDate d) {
        return jpa.lockOpenByStudentIdAndCheckinDate(s, d).stream().findFirst().map(this::toDomain);
    }

    @Override public List<StudentCheckin> findByStudentIdAndDate(Long s, LocalDate d) {
        return jpa.findByStudentIdAndCheckinDate(s, d).stream().map(this::toDomain).toList();
    }

    @Override public List<StudentCheckin> findByClassIdAndDate(Long classId, LocalDate d) {
        return jpa.findByClassIdAndCheckinDate(classId, d).stream().map(this::toDomain).toList();
    }

    private StudentCheckin toDomain(StudentCheckinJpaEntity e) {
        return StudentCheckin.builder().id(e.getId()).tenantId(e.getTenantId()).studentId(e.getStudentId())
                .classId(e.getClassId()).checkinDate(e.getCheckinDate()).scheduleSlotId(e.getScheduleSlotId())
                .sessionStart(e.getSessionStart()).sessionEnd(e.getSessionEnd())
                .checkInTime(e.getCheckInTime()).checkOutTime(e.getCheckOutTime())
                .lateMinutes(e.getLateMinutes()).earlyMinutes(e.getEarlyMinutes()).createdAt(e.getCreatedAt()).build();
    }

    private StudentCheckinJpaEntity toEntity(StudentCheckin c) {
        StudentCheckinJpaEntity e = new StudentCheckinJpaEntity();
        e.setId(c.getId()); e.setTenantId(c.getTenantId()); e.setStudentId(c.getStudentId()); e.setClassId(c.getClassId());
        e.setCheckinDate(c.getCheckinDate()); e.setScheduleSlotId(c.getScheduleSlotId());
        e.setSessionStart(c.getSessionStart()); e.setSessionEnd(c.getSessionEnd());
        e.setCheckInTime(c.getCheckInTime()); e.setCheckOutTime(c.getCheckOutTime());
        e.setLateMinutes(c.getLateMinutes() != null ? c.getLateMinutes() : 0);
        e.setEarlyMinutes(c.getEarlyMinutes() != null ? c.getEarlyMinutes() : 0);
        e.setCreatedAt(c.getCreatedAt());
        return e;
    }
}
