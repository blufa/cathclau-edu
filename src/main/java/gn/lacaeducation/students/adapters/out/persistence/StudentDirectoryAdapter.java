package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.application.port.out.StudentDirectoryPort;
import gn.lacaeducation.students.domain.StudentDirectoryEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class StudentDirectoryAdapter implements StudentDirectoryPort {

    private final StudentDirectoryJpaRepository jpa;

    @Override
    public List<StudentDirectoryEntry> search(String matricule, String nameQuery) {
        return jpa.searchDirectory(matricule == null ? "" : matricule, nameQuery == null ? "" : nameQuery)
                .stream()
                .map(r -> new StudentDirectoryEntry(r.getFirstName(), r.getLastName(), r.getStudentNumber(),
                        r.getBirthDate(), r.getOriginSchool(), r.getSchoolName(), r.getLastClass(), r.getLastDecision(),
                        r.getUnpaidMonths() == null ? 0 : r.getUnpaidMonths().intValue(), r.getOverallAverage()))
                .toList();
    }

    @Override
    public List<String> searchSchools(String query) {
        return jpa.searchSchools(query == null ? "" : query);
    }

    @Override
    public List<String> searchPreviousClasses(String query) {
        return jpa.searchPreviousClasses(query == null ? "" : query);
    }
}
