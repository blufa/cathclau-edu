package gn.lacaeducation.lessons.adapters.out.persistence;

import gn.lacaeducation.lessons.TeacherTeachingProvider;
import gn.lacaeducation.lessons.domain.LessonStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class TeacherTeachingProviderAdapter implements TeacherTeachingProvider {

    private final LessonEntryJpaRepository jpa;

    @Override
    public int realizedMinutes(Long teacherId, LocalDate from, LocalDate to) {
        return jpa.findByTeacherIdAndStatusAndLessonDateBetween(teacherId, LessonStatus.DONE, from, to).stream()
                .mapToInt(l -> l.getDurationMinutes() != null ? l.getDurationMinutes() : 0)
                .sum();
    }

    @Override
    public Map<Long, Integer> realizedMinutesByClass(Long teacherId, LocalDate from, LocalDate to) {
        return jpa.findByTeacherIdAndStatusAndLessonDateBetween(teacherId, LessonStatus.DONE, from, to).stream()
                .collect(Collectors.groupingBy(LessonEntryJpaEntity::getClassId,
                        Collectors.summingInt(l -> l.getDurationMinutes() != null ? l.getDurationMinutes() : 0)));
    }

    @Override
    public Map<LocalDate, Map<Long, Integer>> realizedMinutesByClassAndDate(Long teacherId, LocalDate from, LocalDate to) {
        return jpa.findByTeacherIdAndStatusAndLessonDateBetween(teacherId, LessonStatus.DONE, from, to).stream()
                .collect(Collectors.groupingBy(LessonEntryJpaEntity::getLessonDate,
                        Collectors.groupingBy(LessonEntryJpaEntity::getClassId,
                                Collectors.summingInt(l -> l.getDurationMinutes() != null ? l.getDurationMinutes() : 0))));
    }
}
