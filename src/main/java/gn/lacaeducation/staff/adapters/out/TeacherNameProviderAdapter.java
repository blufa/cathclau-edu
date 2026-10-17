package gn.lacaeducation.staff.adapters.out;

import gn.lacaeducation.grading.TeacherNameProvider;
import gn.lacaeducation.staff.application.port.out.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class TeacherNameProviderAdapter implements TeacherNameProvider {

    private final TeacherRepository teacherRepository;

    @Override
    public Optional<String> teacherName(Long teacherId) {
        if (teacherId == null) return Optional.empty();
        return teacherRepository.findById(teacherId)
                .map(t -> (t.getFirstName() + " " + t.getLastName()).trim());
    }
}
