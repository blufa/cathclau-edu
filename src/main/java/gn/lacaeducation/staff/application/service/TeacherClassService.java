package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.staff.application.port.out.TeacherClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Affectation des classes enseignées par un enseignant (checklist sur sa fiche). */
@Service
@Transactional
@RequiredArgsConstructor
public class TeacherClassService {

    private final TeacherClassRepository repository;

    @Transactional(readOnly = true)
    public List<Long> findClassIds(Long teacherId) {
        return repository.findClassIds(teacherId);
    }

    public void setClasses(Long teacherId, List<Long> classIds) {
        repository.replace(teacherId, TenantContext.get(), classIds);
    }
}
