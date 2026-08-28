package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.staff.application.port.out.TeacherLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Affectation des niveaux (cycles) où un enseignant est habilité à enseigner (liste déroulante sur sa
 * fiche) — même modèle que TeacherSubjectService. */
@Service
@Transactional
@RequiredArgsConstructor
public class TeacherLevelService {

    private final TeacherLevelRepository repository;

    @Transactional(readOnly = true)
    public List<Long> findLevelIds(Long teacherId) {
        return repository.findLevelIds(teacherId);
    }

    @Transactional(readOnly = true)
    public List<Long> findTeacherIdsByLevel(Long levelId) {
        return repository.findTeacherIds(levelId);
    }

    public void setLevels(Long teacherId, List<Long> levelIds) {
        repository.replace(teacherId, TenantContext.get(), levelIds);
    }
}
