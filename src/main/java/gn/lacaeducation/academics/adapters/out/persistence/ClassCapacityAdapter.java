package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.application.port.out.ClasseRepository;
import gn.lacaeducation.students.ClassCapacityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Provides the students module with class capacity (owned by academics), implementing the students
 * outbound port. Dependency direction is academics -> students, consistent with the enrollment-event
 * listener, so no module cycle is introduced.
 */
@Component
@RequiredArgsConstructor
public class ClassCapacityAdapter implements ClassCapacityPort {

    private final ClasseRepository classeRepository;

    @Override
    public Integer maxCapacity(Long classId) {
        return classeRepository.findById(classId)
                .map(c -> c.getMaxCapacity())
                .orElse(null);
    }

    @Override
    public Integer lockAndGetMaxCapacity(Long classId) {
        return classeRepository.lockById(classId)
                .map(c -> c.getMaxCapacity())
                .orElse(null);
    }
}
