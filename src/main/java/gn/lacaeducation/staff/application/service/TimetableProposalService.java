package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.staff.application.port.out.TeacherAvailabilityRepository;
import gn.lacaeducation.staff.domain.TeacherAvailability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Génère une PROPOSITION d'emploi du temps à partir des disponibilités des enseignants et des
 * affectations classe-matière (AcademicsDataProvider). Algorithme glouton : pour chaque classe et
 * chaque matière, place N séances par semaine sur des créneaux où l'enseignant est disponible et où
 * ni la classe ni l'enseignant ne sont déjà occupés. Ne persiste rien — l'application des créneaux se
 * fait via l'endpoint de création existant (qui revérifie les conflits).
 */
@Service
@RequiredArgsConstructor
public class TimetableProposalService {

    private final AcademicsDataProvider academics;
    private final TeacherAvailabilityRepository availabilityRepo;

    /** Jours ouvrés standard et créneaux horaires par défaut. */
    private static final List<String> DAYS = List.of("LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI", "SAMEDI");
    private static final List<LocalTime[]> PERIODS = List.of(
            new LocalTime[]{LocalTime.of(8, 0), LocalTime.of(9, 0)},
            new LocalTime[]{LocalTime.of(9, 0), LocalTime.of(10, 0)},
            new LocalTime[]{LocalTime.of(10, 15), LocalTime.of(11, 15)},
            new LocalTime[]{LocalTime.of(11, 15), LocalTime.of(12, 15)},
            new LocalTime[]{LocalTime.of(15, 0), LocalTime.of(16, 0)},
            new LocalTime[]{LocalTime.of(16, 0), LocalTime.of(17, 0)});

    public record ProposedSlot(Long classId, Long subjectId, Long teacherId, String day,
                               String startTime, String endTime) {}
    /** Une affectation classe-matière de l'enseignant (pour répartir ses créneaux disponibles). */
    private record TeacherSubject(Long classId, Long subjectId) {}
    public record TimetableProposal(List<ProposedSlot> slots, List<Long> teachersWithoutAvailability) {}

    /**
     * Génère l'emploi du temps directement à partir des disponibilités : chaque créneau où un
     * enseignant est disponible est rempli par l'une des classes/matières qu'il enseigne (en rotation),
     * sans qu'une classe soit occupée deux fois au même moment. Le nombre de séances par matière
     * découle des disponibilités déclarées — il n'est pas saisi.
     */
    @Transactional(readOnly = true)
    public TimetableProposal propose(List<Long> classIds) {
        // Regroupe par enseignant les couples (classe, matière) qu'il enseigne dans les classes ciblées.
        Map<Long, List<TeacherSubject>> byTeacher = new java.util.LinkedHashMap<>();
        for (Long classId : classIds) {
            for (ClassSubjectPublicInfo cs : academics.findClassSubjects(classId)) {
                if (cs.teacherId() == null) continue;
                byTeacher.computeIfAbsent(cs.teacherId(), k -> new ArrayList<>())
                        .add(new TeacherSubject(classId, cs.subjectId()));
            }
        }

        List<ProposedSlot> slots = new ArrayList<>();
        List<Long> noAvailability = new ArrayList<>();
        Set<String> classBusy = new HashSet<>();    // classId|day|period
        Set<String> teacherBusy = new HashSet<>();  // teacherId|day|period

        // Conflits avec l'existant.
        for (Long classId : classIds) {
            academics.findScheduleByClass(classId).forEach(s -> {
                int p = periodIndex(s.startTime());
                if (p >= 0) {
                    classBusy.add(key(classId, s.day(), p));
                    if (s.teacherId() != null) teacherBusy.add(key(s.teacherId(), s.day(), p));
                }
            });
        }

        for (Map.Entry<Long, List<TeacherSubject>> e : byTeacher.entrySet()) {
            Long teacherId = e.getKey();
            List<TeacherSubject> subjects = e.getValue();
            List<TeacherAvailability> avail = availabilityRepo.findByTeacherId(teacherId);
            if (avail.isEmpty()) { noAvailability.add(teacherId); continue; }
            int rotation = 0;
            for (String day : DAYS) {
                for (int p = 0; p < PERIODS.size(); p++) {
                    LocalTime start = PERIODS.get(p)[0], end = PERIODS.get(p)[1];
                    if (!available(avail, day, start, end)) continue;       // créneau hors disponibilité
                    if (teacherBusy.contains(key(teacherId, day, p))) continue;
                    // Choisit une classe/matière de l'enseignant dont la classe est libre à ce créneau.
                    for (int k = 0; k < subjects.size(); k++) {
                        TeacherSubject ts = subjects.get((rotation + k) % subjects.size());
                        if (classBusy.contains(key(ts.classId(), day, p))) continue;
                        slots.add(new ProposedSlot(ts.classId(), ts.subjectId(), teacherId, day, start.toString(), end.toString()));
                        classBusy.add(key(ts.classId(), day, p));
                        teacherBusy.add(key(teacherId, day, p));
                        rotation = (rotation + k + 1) % subjects.size();
                        break;
                    }
                }
            }
        }
        return new TimetableProposal(slots, noAvailability);
    }

    /** Un enseignant sans disponibilité déclarée est considéré disponible sur tous les créneaux standard. */
    private boolean available(List<TeacherAvailability> avail, String day, LocalTime start, LocalTime end) {
        if (avail.isEmpty()) return true;
        return avail.stream().anyMatch(a -> day.equalsIgnoreCase(a.getDay())
                && a.getStartTime() != null && a.getEndTime() != null
                && !a.getStartTime().isAfter(start) && !a.getEndTime().isBefore(end));
    }

    private int periodIndex(String startTime) {
        if (startTime == null) return -1;
        for (int p = 0; p < PERIODS.size(); p++) {
            if (startTime.startsWith(PERIODS.get(p)[0].toString())) return p;
        }
        return -1;
    }

    private String key(Long id, String day, int period) { return id + "|" + day + "|" + period; }
}
