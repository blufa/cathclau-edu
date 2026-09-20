package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.application.port.out.TeacherScheduleFlagPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TeacherScheduleFlagAdapter implements TeacherScheduleFlagPort {

    private final TeacherFlagJpaRepository repo;

    @Override
    public boolean allowsConcurrentClasses(Long teacherId) {
        if (teacherId == null) return false;
        return repo.findById(teacherId).map(TeacherFlagJpaEntity::isAllowsConcurrentClasses).orElse(false);
    }
}
