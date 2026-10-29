package gn.lacaeducation.staff.adapters.out;

import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.staff.application.port.out.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class TeacherPublicInfoProviderAdapter implements TeacherPublicInfoProvider {

    private final TeacherRepository teacherRepository;

    @Override
    public Optional<TeacherInfo> findTeacher(Long teacherId) {
        return teacherRepository.findById(teacherId).map(t -> new TeacherInfo(
                t.getId(), t.getFirstName(), t.getLastName(), t.getStaffNumber(),
                t.getSpecialization(), t.getEmail(), t.getPhone(), t.getPhoto(),
                t.getRemunerationType(), t.getRemunerationRate()));
    }

    @Override
    public List<TeacherInfo> listTeachers(Long tenantId) {
        return teacherRepository.findByTenantId(tenantId).stream()
                .map(t -> new TeacherInfo(t.getId(), t.getFirstName(), t.getLastName(), t.getStaffNumber(),
                        t.getSpecialization(), t.getEmail(), t.getPhone(), t.getPhoto(),
                        t.getRemunerationType(), t.getRemunerationRate()))
                .toList();
    }

    @Override
    public Optional<Long> findTeacherIdByUserId(Long userId) {
        if (userId == null) return Optional.empty();
        return teacherRepository.findByUserId(userId).map(t -> t.getId());
    }

    @Override
    public Optional<Long> findUserIdByTeacherId(Long teacherId) {
        if (teacherId == null) return Optional.empty();
        return teacherRepository.findById(teacherId).map(t -> t.getUserId());
    }
}
