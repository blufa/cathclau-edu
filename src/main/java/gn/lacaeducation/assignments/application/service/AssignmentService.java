package gn.lacaeducation.assignments.application.service;

import gn.lacaeducation.assignments.application.port.out.AssignmentRepository;
import gn.lacaeducation.assignments.application.port.out.AssignmentSubmissionRepository;
import gn.lacaeducation.assignments.domain.Assignment;
import gn.lacaeducation.assignments.domain.AssignmentStatus;
import gn.lacaeducation.assignments.domain.AssignmentSubmission;
import gn.lacaeducation.assignments.domain.SubmissionStatus;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Devoirs (Assignment) : un enseignant crée un devoir pour une classe/matière avec une échéance ;
 * les élèves rendent (à temps ou en retard), puis l'enseignant note. Module autonome qui ne référence
 * les classes / matières / élèves que par identifiant (aucune duplication des modules existants).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository repo;
    private final AssignmentSubmissionRepository submissionRepo;

    public record CreateAssignmentCommand(Long tenantId, Long classId, Long subjectId, Long teacherId,
                                          String title, String instructions, String attachmentUrl,
                                          LocalDate dueDate, BigDecimal maxScore) {}

    public Assignment create(CreateAssignmentCommand c) {
        if (c.title() == null || c.title().isBlank()) throw new BusinessRuleException("Le titre du devoir est requis");
        if (c.classId() == null || c.subjectId() == null) throw new BusinessRuleException("Classe et matière requises");
        return repo.save(Assignment.builder()
                .tenantId(c.tenantId()).classId(c.classId()).subjectId(c.subjectId()).teacherId(c.teacherId())
                .title(c.title()).instructions(c.instructions()).attachmentUrl(c.attachmentUrl())
                .dueDate(c.dueDate()).maxScore(c.maxScore())
                .status(AssignmentStatus.OPEN).createdAt(LocalDateTime.now()).build());
    }

    public Assignment update(Long id, CreateAssignmentCommand c) {
        Assignment a = load(id);
        a.setClassId(c.classId()); a.setSubjectId(c.subjectId()); a.setTeacherId(c.teacherId());
        a.setTitle(c.title()); a.setInstructions(c.instructions()); a.setAttachmentUrl(c.attachmentUrl());
        a.setDueDate(c.dueDate()); a.setMaxScore(c.maxScore());
        return repo.save(a);
    }

    public Assignment close(Long id) {
        Assignment a = load(id);
        a.setStatus(AssignmentStatus.CLOSED);
        return repo.save(a);
    }

    public Assignment reopen(Long id) {
        Assignment a = load(id);
        a.setStatus(AssignmentStatus.OPEN);
        return repo.save(a);
    }

    public void delete(Long id) { repo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<Assignment> find(Long tenantId, Long classId, AssignmentStatus status) {
        if (classId != null) return repo.findByTenantIdAndClassId(tenantId, classId);
        if (status != null) return repo.findByTenantIdAndStatus(tenantId, status);
        return repo.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Assignment findById(Long id) { return load(id); }

    // ── Rendus des élèves ────────────────────────────────────────────────────

    public AssignmentSubmission submit(Long tenantId, Long assignmentId, Long studentId, String fileUrl) {
        Assignment a = load(assignmentId);
        if (a.getStatus() == AssignmentStatus.CLOSED) {
            throw new BusinessRuleException("Ce devoir est clôturé : plus aucun rendu n'est accepté");
        }
        boolean late = a.getDueDate() != null && LocalDate.now().isAfter(a.getDueDate());
        AssignmentSubmission s = submissionRepo.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseGet(() -> AssignmentSubmission.builder()
                        .tenantId(tenantId).assignmentId(assignmentId).studentId(studentId).build());
        s.setFileUrl(fileUrl);
        s.setSubmittedAt(LocalDateTime.now());
        s.setStatus(late ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED);
        return submissionRepo.save(s);
    }

    public AssignmentSubmission grade(Long submissionId, BigDecimal grade, String feedback) {
        AssignmentSubmission s = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("AssignmentSubmission", submissionId));
        Assignment a = load(s.getAssignmentId());
        if (a.getMaxScore() != null && grade != null && grade.compareTo(a.getMaxScore()) > 0) {
            throw new BusinessRuleException("La note dépasse le barème du devoir (" + a.getMaxScore() + ")");
        }
        s.setGrade(grade);
        s.setFeedback(feedback);
        s.setStatus(SubmissionStatus.GRADED);
        return submissionRepo.save(s);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmission> submissions(Long assignmentId) {
        return submissionRepo.findByAssignmentId(assignmentId);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmission> submissionsByStudent(Long studentId) {
        return submissionRepo.findByStudentId(studentId);
    }

    private Assignment load(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
    }
}
