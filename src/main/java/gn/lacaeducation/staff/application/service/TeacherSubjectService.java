package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.staff.application.port.out.TeacherSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Affectation des matières enseignées par un enseignant (liste déroulante sur sa fiche). */
@Service
@Transactional
@RequiredArgsConstructor
public class TeacherSubjectService {

    private final TeacherSubjectRepository repository;

    @Transactional(readOnly = true)
    public List<Long> findSubjectIds(Long teacherId) {
        return repository.findSubjectIds(teacherId);
    }

    @Transactional(readOnly = true)
    public List<Long> findTeacherIdsBySubject(Long subjectId) {
        return repository.findTeacherIds(subjectId);
    }

    public void setSubjects(Long teacherId, List<Long> subjectIds) {
        repository.replace(teacherId, TenantContext.get(), subjectIds);
    }
}
