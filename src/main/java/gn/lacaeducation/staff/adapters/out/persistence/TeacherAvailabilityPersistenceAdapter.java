package gn.lacaeducation.staff.adapters.out.persistence;

import gn.lacaeducation.staff.application.port.out.TeacherAvailabilityRepository;
import gn.lacaeducation.staff.domain.TeacherAvailability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class TeacherAvailabilityPersistenceAdapter implements TeacherAvailabilityRepository {

    private final TeacherAvailabilityJpaRepository jpa;

    @Override
    public TeacherAvailability save(TeacherAvailability a) {
        TeacherAvailabilityJpaEntity e = new TeacherAvailabilityJpaEntity();
        e.setId(a.getId());
        e.setTenantId(a.getTenantId());
        e.setTeacherId(a.getTeacherId());
        e.setDay(a.getDay());
        e.setStartTime(a.getStartTime());
        e.setEndTime(a.getEndTime());
        e.setSchoolClassId(a.getSchoolClassId());
        e.setSubjectId(a.getSubjectId());
        return toDomain(jpa.save(e));
    }

    @Override
    public List<TeacherAvailability> findByTeacherId(Long teacherId) {
        return jpa.findByTeacherIdOrderByDayAscStartTimeAsc(teacherId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private TeacherAvailability toDomain(TeacherAvailabilityJpaEntity e) {
        return TeacherAvailability.builder()
                .id(e.getId()).tenantId(e.getTenantId()).teacherId(e.getTeacherId())
                .day(e.getDay()).startTime(e.getStartTime()).endTime(e.getEndTime())
                .schoolClassId(e.getSchoolClassId()).subjectId(e.getSubjectId())
                .build();
    }
}
