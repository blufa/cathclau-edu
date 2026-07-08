package gn.lacaeducation.lessons.adapters.out.persistence;

import gn.lacaeducation.lessons.TeacherTeachingProvider;
import gn.lacaeducation.lessons.domain.LessonStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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
}
