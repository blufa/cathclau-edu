package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererClasseUseCase;
import gn.lacaeducation.academics.application.port.out.AnneeScolaireRepository;
import gn.lacaeducation.academics.application.port.out.ClasseRepository;
import gn.lacaeducation.academics.application.port.out.EnrollmentQueryPort;
import gn.lacaeducation.academics.application.port.out.ReferentielRepository;
import gn.lacaeducation.academics.domain.AnneeScolaire;
import gn.lacaeducation.academics.domain.Classe;
import gn.lacaeducation.academics.domain.ClasseMatiere;
import gn.lacaeducation.academics.domain.ClassStudent;
import gn.lacaeducation.academics.domain.Matiere;
import gn.lacaeducation.academics.domain.valueobject.StatutAnneeScolaire;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ClasseService implements GererClasseUseCase {

    private final ClasseRepository classeRepository;
    private final EnrollmentQueryPort enrollmentQueryPort;
    private final AnneeScolaireRepository anneeRepository;
    private final ReferentielRepository referentielRepository;

    @Override
    public Classe create(CreateClassCommand command) {
        requireWritableYear(command.academicYearId());
        Classe classe = Classe.builder()
                .tenantId(command.tenantId())
                .levelId(command.levelId())
                .academicYearId(command.academicYearId())
                .headTeacherId(command.headTeacherId())
                .roomId(command.roomId())
                .label(command.label())
                .headcount(0)
                .maxCapacity(command.maxCapacity())
                .classType(command.classType() != null ? command.classType() : gn.lacaeducation.academics.domain.valueobject.ClassType.NORMALE)
                .build();
        return classeRepository.save(classe);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Classe> findById(Long id) {
        return classeRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Classe> findByAcademicYear(Long academicYearId) {
        return classeRepository.findByAcademicYearId(academicYearId);
    }

    @Override
    public ClasseMatiere assignSubject(Long classId, Long subjectId, Long teacherId, java.math.BigDecimal coefficient, String code) {
        // Idempotent : si la matière est déjà affectée à la classe (contrainte unique classe+matière),
        // on met à jour l'enseignant (et le coefficient si fourni) au lieu d'insérer un doublon.
        ClasseMatiere existing = classeRepository.findSubjectsByClassId(classId).stream()
                .filter(cm -> subjectId.equals(cm.getSubjectId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            boolean teacherChanged = !Objects.equals(existing.getTeacherId(), teacherId);
            existing.setTeacherId(teacherId);
            if (coefficient != null) {
                existing.setCoefficient(coefficient);
            }
            if (code != null && !code.isBlank()) {
                // Code choisi explicitement (le Directeur des Études peut toujours le modifier).
                existing.setCode(code.trim());
            } else if (teacherChanged) {
                // Le professeur affecté change : son ancien code ne le concerne plus.
                existing.setCode(resolveTeacherCode(subjectId, teacherId));
            }
            return classeRepository.saveClassSubject(existing);
        }
        String resolvedCode = code != null && !code.isBlank() ? code.trim() : resolveTeacherCode(subjectId, teacherId);
        ClasseMatiere cm = ClasseMatiere.builder()
                .schoolClassId(classId)
                .subjectId(subjectId)
                .teacherId(teacherId)
                .coefficient(coefficient)
                .code(resolvedCode)
                .build();
        return classeRepository.saveClassSubject(cm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClasseMatiere> findTeacherCodesForSubject(Long subjectId) {
        return classeRepository.findSubjectsBySubjectId(subjectId);
    }

    /**
     * Détermine le code enseignant pour cette matière : réutilise celui déjà attribué à ce
     * professeur pour cette matière ailleurs dans l'école (cohérence inter-classes), sinon génère
     * le prochain code disponible ({@code <abréviation matière><n>}, ex. "CH1", "CH2"...). Toujours
     * modifiable ensuite par le Directeur des Études via le paramètre {@code code}.
     */
    private String resolveTeacherCode(Long subjectId, Long teacherId) {
        if (teacherId == null) return null;
        List<ClasseMatiere> subjectAssignments = classeRepository.findSubjectsBySubjectId(subjectId);
        String existingForTeacher = subjectAssignments.stream()
                .filter(cm -> teacherId.equals(cm.getTeacherId()) && cm.getCode() != null && !cm.getCode().isBlank())
                .map(ClasseMatiere::getCode)
                .findFirst()
                .orElse(null);
        if (existingForTeacher != null) return existingForTeacher;

        String label = referentielRepository.findSubjectById(subjectId).map(Matiere::getLabel).orElse(null);
        String prefix = subjectAbbreviation(label);
        Set<String> used = subjectAssignments.stream()
                .map(ClasseMatiere::getCode)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.toSet());
        int n = 1;
        while (used.contains(prefix + n)) n++;
        return prefix + n;
    }

    /** Abréviation par défaut d'une matière (2 lettres, sans accent) — ex. "Chimie" → "CH". */
    private static String subjectAbbreviation(String label) {
        if (label == null || label.isBlank()) return "MA";
        String letters = Normalizer.normalize(label.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z]", "");
        if (letters.isEmpty()) return "MA";
        return letters.substring(0, Math.min(2, letters.length())).toUpperCase();
    }

    @Override
    public Classe update(Long id, CreateClassCommand command) {
        Classe c = classeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Class", id));
        requireWritableYear(c.getAcademicYearId());
        c.setLevelId(command.levelId()); c.setAcademicYearId(command.academicYearId());
        c.setHeadTeacherId(command.headTeacherId()); c.setRoomId(command.roomId());
        c.setLabel(command.label()); c.setMaxCapacity(command.maxCapacity());
        if (command.classType() != null) c.setClassType(command.classType());
        return classeRepository.save(c);
    }

    @Override @Transactional(readOnly=true)
    public List<Classe> findByTenantId(Long tenantId) { return classeRepository.findByTenantId(tenantId); }

    @Override public void removeSubject(Long classSubjectId) { classeRepository.deleteClassSubjectById(classSubjectId); }

    @Override public void delete(Long id) { classeRepository.deleteById(id); }

    @Override @Transactional(readOnly = true)
    public List<ClasseMatiere> findSubjects(Long classId) { return classeRepository.findSubjectsByClassId(classId); }

    @Override @Transactional(readOnly = true)
    public List<ClassStudent> findStudentsByClassId(Long classId) {
        return classeRepository.findStudentsByClassId(classId);
    }

    /** Classes cannot be created/modified on a CLOTUREE or ARCHIVEE academic year (read-only). */
    private void requireWritableYear(Long academicYearId) {
        if (academicYearId == null) return;
        AnneeScolaire year = anneeRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", academicYearId));
        if (year.getStatus() == StatutAnneeScolaire.CLOTUREE || year.getStatus() == StatutAnneeScolaire.ARCHIVEE) {
            throw new BusinessRuleException(
                    "Academic year id=" + academicYearId + " is " + year.getStatus() + " (read-only); classes cannot be modified.");
        }
    }

    @Override @Transactional(readOnly = true)
    public EnrollmentCountInfo getEnrollmentCount(Long classId) {
        Classe classe = classeRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", classId));
        long current = enrollmentQueryPort.countValidatedByClassId(classId);
        long available = classe.getMaxCapacity() != null ? classe.getMaxCapacity() - current : Long.MAX_VALUE;
        return new EnrollmentCountInfo(classId, current, classe.getMaxCapacity(), available);
    }
}
