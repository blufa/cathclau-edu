package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.StudentReportProvider;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class StudentReportProviderAdapter implements StudentReportProvider {

    private final ReportCardRepository repo;

    @Override
    public Optional<ReportInfo> latestPublished(Long studentId) {
        return repo.findByStudentId(studentId).stream()
                .filter(r -> r.getStatus() == ReportCardStatus.PUBLISHED)
                .max(Comparator.comparing(ReportCard::getGeneratedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(r -> new ReportInfo(r.getOverallAverage(), r.getRank(), r.getClassSize(),
                        r.getHonor(), r.getDecision()));
    }

    @Override
    public java.util.List<PublishedReportInfo> listPublished(Long studentId) {
        return repo.findByStudentId(studentId).stream()
                .filter(r -> r.getStatus() == ReportCardStatus.PUBLISHED)
                .sorted(Comparator.comparing(ReportCard::getGeneratedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(r -> new PublishedReportInfo(r.getId(), r.getPeriodId(), r.getOverallAverage(), r.getHonor()))
                .toList();
    }
}
