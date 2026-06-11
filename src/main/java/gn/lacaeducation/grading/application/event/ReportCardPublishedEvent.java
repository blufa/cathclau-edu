package gn.lacaeducation.grading.application.event;
public record ReportCardPublishedEvent(Long tenantId, Long studentId, Long periodId, Long reportCardId) {}
