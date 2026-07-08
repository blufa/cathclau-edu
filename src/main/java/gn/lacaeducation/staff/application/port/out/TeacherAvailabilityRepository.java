package gn.lacaeducation.staff.application.port.out;

import gn.lacaeducation.staff.domain.TeacherAvailability;

import java.util.List;

public interface TeacherAvailabilityRepository {
    TeacherAvailability save(TeacherAvailability availability);
    List<TeacherAvailability> findByTeacherId(Long teacherId);
    void deleteById(Long id);
}
